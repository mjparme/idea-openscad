# Contributing

## Pull requests

All PR are welcomed !
If they are linked to an issue please refer to it.

## Releasing to JetBrains Marketplace

1. Create a [JetBrains Marketplace](https://plugins.jetbrains.com/) account and generate a [permanent token](https://plugins.jetbrains.com/author/me/tokens).
2. Add the token as repository secret `JETBRAINS_PUBLISH_TOKEN` in GitHub.
3. Push to `master` to create a SNAPSHOT pre-release artifact via the Build workflow.
4. Run the **PromotePreRelease** workflow manually to publish `1.0.0` (or current release version) to the marketplace.

The plugin ID is `com.mjparme.idea-openscad`. The first publish creates the marketplace listing.

To publish locally instead:

```bash
export PUBLISH_TOKEN=your-token-here
./gradlew publishPlugin
```

Optional signing env vars for marketplace uploads: `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`.

## Test development version from GitHub

GitHub actions automatically build and publish plugin distribution.

To test a specific version, go in the *Code* tab -> *tags* menu. Click on the version you wish to test and download the *idea-openscad-x.x.x[-SNAPSHOT].zip* file.

In IntelliJ, go in *File* -> *Settings...* -> *Plugins* -> ![configuration icon](https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/general/gearPlain.svg?sanitize=true) -> *Install Plugin from Disk...* and select the distribution zip file.

Restart IntelliJ to test development version.
