# Contributing

## Pull requests

All PR are welcomed !
If they are linked to an issue please refer to it.

## Releasing to JetBrains Marketplace

The plugin ID is `com.mjparme.idea-openscad`. The first publish creates the marketplace listing.

1. Set `pluginVersion` in `gradle.properties` and update `CHANGELOG.md` for the release.
2. Build and verify locally:

```bash
./gradlew verifyPlugin buildPlugin
```

3. Upload `build/distributions/idea-openscad-{version}.zip` in the [JetBrains Marketplace](https://plugins.jetbrains.com/) editor.

Or run the **Publish plugin** workflow manually (uses `JETBRAINS_PUBLISH_TOKEN` and `./gradlew publishPlugin`).

To publish from the CLI instead:

```bash
export PUBLISH_TOKEN=your-token-here
./gradlew publishPlugin
```

Optional signing env vars for marketplace uploads: `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`.

## Test a development build locally

Build the plugin distribution:

```bash
./gradlew buildPlugin
```

In IntelliJ, go in *File* -> *Settings...* -> *Plugins* -> ![configuration icon](https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/general/gearPlain.svg?sanitize=true) -> *Install Plugin from Disk...* and select `build/distributions/idea-openscad-{version}.zip`.

Restart IntelliJ to test the development version.
