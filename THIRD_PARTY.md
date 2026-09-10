# Third-party components

This repository builds Ruffle from our own tree (not upstream's release; see
`/root/coordination/agents/flash-air/BRIEF.md`, "Ruffle built from our tree")
and wraps it for Android.

| Component | Version / commit | Licence | Source | Where in tree |
|---|---|---|---|---|
| Ruffle | `plugin/ruffle-0.4.1` line, built from our tree | MIT OR Apache-2.0 (dual) | https://github.com/ruffle-rs/ruffle | entire tree (fork), notice at `LICENSE.md` and `web/LICENSE.md` |
| swf crate (part of Ruffle) | as vendored | MIT OR Apache-2.0 (dual) | https://github.com/ruffle-rs/ruffle (`swf/`) | `swf/LICENSE-MIT`, `swf/LICENSE-APACHE` |
| Enginehost's own Android wrapper | this repository | MIT | https://github.com/Droidtop/enginehost-flash-air-plugin | addendum at the bottom of `LICENSE.md` |

## Obligations

Ruffle is dual MIT/Apache-2.0, both permissive; Enginehost's own wrapper
code is MIT, the most permissive licence compatible with Ruffle's. Games and
any AIR/SWF content played through this plugin are not redistributed by this
repository.
