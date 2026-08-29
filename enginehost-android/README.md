# enginehost Flash/AIR plugin

This Android plugin runs SWF content and the main SWF from an Adobe AIR
captive-runtime layout directly from the supplied game folder. It embeds the
official Ruffle 0.4.1 self-hosted Web release at CI build time and does not use
Wine or redistribute Adobe runtimes.

The first release is experimental. It advertises Flash/AIR engine versions
1.0 through 32.0 for discovery, but Ruffle compatibility varies with the game,
especially for ActionScript 3 and AIR APIs. Later plugin releases can improve
that compatibility without changing a game's mandatory engineVersion.

Network access is disabled by default. Supported options are `allowNetwork`,
`javaScript`, `domStorage`, `database`, and
`mediaPlaybackRequiresGesture`. Entry files and local navigation are confined
to the live game directory.

Ruffle is copyright its contributors and dual-licensed under MIT or
Apache-2.0. CI packages its upstream license texts beside the runtime.
