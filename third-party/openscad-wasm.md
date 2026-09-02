# OpenSCAD WebAssembly (preview runtime)

The split preview editor ships a headless OpenSCAD build compiled to WebAssembly.
These files are bundled inside the plugin JAR under `html/vendor/openscad/`:

| File | Role |
| --- | --- |
| `openscad.js` | ES module loader wrapper |
| `openscad.wasm.js` | Emscripten glue |
| `openscad.wasm` | OpenSCAD WASM binary |

## License

OpenSCAD and the [openscad-wasm](https://github.com/openscad/openscad-wasm) port
are licensed under the **GNU General Public License version 2** (GPL-2.0).

- OpenSCAD: https://github.com/openscad/openscad
- openscad-wasm: https://github.com/openscad/openscad-wasm

Bundling these binaries in the plugin distribution triggers GPL source
distribution obligations for that component.

## Bundled artifact in this repository

CI and release builds use a vendored copy committed as:

`third-party/openscad-wasm-vendor.zip`

That zip is produced from a local openscad-wasm build (see below). It is not
the full openscad-wasm source tree.

## Corresponding source (GPL)

To obtain source matching the bundled WASM:

1. **openscad-wasm build used for the vendor zip** — build from
   https://github.com/openscad/openscad-wasm using Docker/Make as documented in
   that repository, or sync from a sibling checkout:

   ```bash
   cd ../openscad-wasm
   OPENSCAD_REF=master gmake all
   cd ../idea-openscad
   bash scripts/sync-official-openscad-wasm.sh
   ```

2. **OpenSCAD upstream** — the WASM module is a port of OpenSCAD itself:
   https://github.com/openscad/openscad

3. **This plugin repository** — Java/Kotlin plugin code, preview worker JS, and
   webpack bundling live in https://github.com/mjparme/idea-openscad on branch
   `master` (or the release tag for your plugin version).

When you distribute a plugin build that includes `html/vendor/openscad/*`, offer
recipients the GPL source for OpenSCAD/openscad-wasm (URLs above) and identify
the version or commit used to produce the bundled zip.

## Updating the bundled zip

After rebuilding openscad-wasm:

```bash
bash scripts/sync-official-openscad-wasm.sh
cd src/main/javascript/vendor/openscad
zip -j ../../../third-party/openscad-wasm-vendor.zip openscad.js openscad.wasm openscad.wasm.js
```

Record the openscad-wasm commit or OpenSCAD ref in the release changelog when
the vendor zip changes.
