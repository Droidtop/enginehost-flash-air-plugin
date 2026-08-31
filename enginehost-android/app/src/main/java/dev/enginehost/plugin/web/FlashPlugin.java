package dev.enginehost.plugin.web;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import dev.enginehost.api.EngineControllerEvent;
import dev.enginehost.api.EnginePlugin;
import dev.enginehost.api.EnginePluginSession;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.json.JSONObject;

/** In-process Ruffle runtime for SWF and unpacked AIR captive-runtime content. */
public final class FlashPlugin implements EnginePlugin {
    private EnginePluginSession session;
    private WebView webView;
    private File gameRoot;
    private File ruffleRoot;
    private boolean allowNetwork;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(EnginePluginSession session) throws Exception {
        this.session = session;
        if (!"flash_air".equals(session.engine()) ||
            !("swf".equals(session.engineContext()) || "air".equals(session.engineContext()))) {
            throw new IOException("Unsupported Flash/AIR engine context");
        }
        gameRoot = new File(session.gamePath()).getCanonicalFile();
        ruffleRoot = new File(session.bundleDirectory(), "runtime/ruffle").getCanonicalFile();
        if (!gameRoot.isDirectory() || !new File(ruffleRoot, "ruffle.js").isFile()) {
            throw new IOException("Game folder or bundled Ruffle runtime is missing");
        }
        JSONObject options = new JSONObject(session.optionsJson() == null ? "{}" : session.optionsJson());
        allowNetwork = options.optBoolean("allowNetwork", false);
        webView = new WebView(session.host().context());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMediaPlaybackRequiresUserGesture(options.optBoolean("mediaPlaybackRequiresGesture", false));
        settings.setBlockNetworkLoads(!allowNetwork);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new ConfinedClient());
        session.display().addView(webView, new android.view.ViewGroup.LayoutParams(-1, -1));
        load(resolveSwf());
    }

    private void load(File swf) {
        String html = "<!doctype html><meta name=viewport content='width=device-width,initial-scale=1'>" +
            "<style>html,body,#player{margin:0;width:100%;height:100%;background:#000}</style><div id=player></div>" +
            "<script src=" + JSONObject.quote(new File(ruffleRoot, "ruffle.js").toURI().toString()) + "></script>" +
            "<script>addEventListener('load',async()=>{const p=RufflePlayer.newest().createPlayer();" +
            "document.getElementById('player').appendChild(p);await p.load({url:" +
            JSONObject.quote(swf.toURI().toString()) + "});});</script>";
        webView.loadDataWithBaseURL(gameRoot.toURI().toString(), html, "text/html", "UTF-8", null);
    }

    private File resolveSwf() throws Exception {
        String requested = session.execFile();
        if (requested != null && !requested.isBlank()) return confinedGameFile(requested);
        File descriptor = new File(gameRoot, "META-INF/AIR/application.xml");
        if (descriptor.isFile()) {
            String xml;
            try (FileInputStream input = new FileInputStream(descriptor)) {
                byte[] bytes = new byte[(int) Math.min(descriptor.length(), 1024 * 1024)];
                int count = input.read(bytes);
                xml = new String(bytes, 0, Math.max(0, count), java.nio.charset.StandardCharsets.UTF_8);
            }
            java.util.regex.Matcher match = java.util.regex.Pattern.compile("<content>\\s*([^<]+)\\s*</content>").matcher(xml);
            if (match.find()) return confinedGameFile(match.group(1).trim());
        }
        File[] files = gameRoot.listFiles((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".swf"));
        if (files != null && files.length == 1) return files[0].getCanonicalFile();
        throw new IOException("Set execFile to the main SWF");
    }

    private File confinedGameFile(String relative) throws IOException {
        if (new File(relative).isAbsolute()) throw new IOException("execFile must be relative");
        File file = new File(gameRoot, relative).getCanonicalFile();
        if (!file.isFile() || !file.getPath().startsWith(gameRoot.getPath() + File.separator)) {
            throw new IOException("SWF leaves the game folder");
        }
        return file;
    }

    private boolean allowed(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null || "about".equals(scheme) || "data".equals(scheme) || "blob".equals(scheme)) return true;
        if ("http".equals(scheme) || "https".equals(scheme)) return allowNetwork;
        if (!"file".equals(scheme)) return false;
        try {
            File file = new File(uri.getPath() == null ? "" : uri.getPath()).getCanonicalFile();
            return inside(file, gameRoot) || inside(file, ruffleRoot);
        } catch (IOException ignored) { return false; }
    }

    private boolean inside(File file, File root) {
        return file.equals(root) || file.getPath().startsWith(root.getPath() + File.separator);
    }

    private final class ConfinedClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return !allowed(request.getUrl()); }
        @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return allowed(request.getUrl()) ? super.shouldInterceptRequest(view, request) :
                new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
        }
    }

    @Override public boolean onControllerEvent(EngineControllerEvent event) {
        int key = switch (event.action()) {
            case "up" -> KeyEvent.KEYCODE_DPAD_UP;
            case "down" -> KeyEvent.KEYCODE_DPAD_DOWN;
            case "left" -> KeyEvent.KEYCODE_DPAD_LEFT;
            case "right" -> KeyEvent.KEYCODE_DPAD_RIGHT;
            case "confirm" -> KeyEvent.KEYCODE_BUTTON_A;
            case "cancel" -> KeyEvent.KEYCODE_BUTTON_B;
            case "menu" -> KeyEvent.KEYCODE_MENU;
            default -> KeyEvent.KEYCODE_UNKNOWN;
        };
        if (key == KeyEvent.KEYCODE_UNKNOWN) return false;
        int action = event.pressed() ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
        return webView.dispatchKeyEvent(new KeyEvent(event.eventTime(), event.eventTime(), action, key, 0));
    }

    @Override public void onResume() { webView.onResume(); }
    @Override public void onPause() { webView.onPause(); }
    @Override public void onDestroy() { if (webView != null) { webView.destroy(); webView = null; } }
}
