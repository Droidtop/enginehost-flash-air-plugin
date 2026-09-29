# Changelog

All notable changes to this plugin are documented in this file. The format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

This is the plugin's first changelog; the entries below summarize the
wrapper's history to date, drawn from this repository's own commits rather
than the upstream engine's.

## [Unreleased]

### Added

- In-process bundle that plays SWF content and the main movie of an AIR
  captive-runtime layout straight from the game folder, with the Ruffle
  0.4.1 web runtime embedded at build time and its license texts
  packaged beside it.
- Experimental discovery range of Flash/AIR engine versions 1.0 through
  32.0: compatibility varies with the movie, especially for ActionScript 3
  and AIR APIs.
- Declared options `allowNetwork` (off by default) and
  `mediaPlaybackRequiresGesture`, so the config editor offers labelled
  switches instead of blind JSON fields.
- Origin document declaring which engine this repository implements
  (flash_air, swf and air contexts) and which implementation it is
  (Ruffle).
- Release channels: every green push publishes a signed bundle to the
  rolling unstable pre-release, and a manual dispatch promotes a proven
  build to testing or stable.
- Publishing dispatches the plugins index to re-index within a minute
  instead of waiting for its six-hourly schedule.

### Changed

- Signing moved into Enginehost's pinned `sign-engine-bundle.yml` job
  instead of running beside the build: the build job previously held the
  signing key while it ran Gradle and fetched third-party actions by
  moving tag, any of which could have taken the key.
- Bundles are numbered `<declared>-<N>` from a counter over the
  repository's releases, so a rerun cannot repeat a version and a new
  declared version restarts at 1.
- Repository signing key re-certified under the new trust root; bundles
  signed under the previous key no longer verify and had to be rebuilt.
- Release tags are pinned to the exact commit each build was made from,
  and testing rolls like unstable instead of accumulating uploads.
- Versioning starts at 0.1 rather than 1.0: no build has been lived with
  long enough to be called 1.0, and the line stays 0.1 until it is
  promoted to stable.
- The bundled runtime's resources are served confined to the live game
  directory.
- Removed the two dead activities nothing could start.
- Superseded CI runs are cancelled instead of racing each other, so an
  iterative fix session spends one build per push.
- Re-pinned the shared signing workflow after the branch history was
  rewritten to the wrapper changeset.

### Fixed

- Game options are read under the intent key the host actually sends, so
  `allowNetwork` and `mediaPlaybackRequiresGesture` reach the player
  instead of being silently ignored.
- Windows-style paths in AIR application descriptors are normalized.
- A push that uploads no bundle artifact is nothing to publish, not a
  failed unstable job.
- CI requests no SDK packages at setup; the obsolete `tools` package it
  used to ask for has been withdrawn, and asking for it killed the job
  before Gradle ran.
