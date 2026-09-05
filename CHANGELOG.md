<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# OpenSCAD Intellij plugin ChangeLog

## [Unreleased]

### Changed

- Plugin Verifier also checks IntelliJ IDEA 2026.2.1 in addition to `recommended()` latest patches per major

## [1.4.1] - 2026-09-05

### Fixed

- List-comprehension `let` bindings resolve when chained as sibling prefix clauses (e.g. `let(end = …) let(degreesPerPoint = …) for (…) let(angle = …)`)

## [1.4.0] - 2026-09-03

### Added

- Preview corner axis orientation widget (OpenSCAD-style labeled X/Y/Z triad) that tracks camera rotation
- Preview view cube (click a face for Front/Back/Left/Right/Top/Bottom) that tracks camera rotation
- Preview grid uses 1-2-5-10 model-unit cells, is origin-centered, covers the model, and snaps extent so major lines stay aligned
- Preview grid corner legend showing cell size in model units
- Preview grid major-tick coordinate labels along the outer X and Y edges (occluded by the model when it covers them)
- Preview toolbar **Toggle Grid Labels** (disabled while the grid is hidden)
- Preview background dropdown includes the remaining OpenSCAD 3D-view schemes (Metallic, Sunset, Starnight, BeforeDawn, Nature, Daylight Gem, Nocturnal Gem, DeepOcean, Solarized, Tomorrow, Tomorrow Night) plus existing Clear Sky, Cornfield, and plugin Dark Gradient

### Changed

- Preview background menu is sorted alphabetically by name
- Axis orientation widget and grid-scale legend sit above the OpenSCAD output console when the console is expanded

### Fixed

- Preview font extension check uses `FileUtilRt.getExtension` instead of deprecated `FileUtil.getExtension` (Plugin Verifier scheduled-for-removal warning)

## [1.3.0] - 2026-09-02

### Added

- WASM preview lazy-loads the openscad-wasm **Liberation** font bundle (~8 MB) when bundled sources use `text()` or call `textmetrics()` / `fontmetrics()`; includes Liberation Sans, Serif, and Mono (regular/bold/italic). Models without text skip the download. Experimental builtins also pass `--enable=textmetrics`.
- **WASM preview font directories** setting: scan configured folders for `.ttf`/`.otf`/`.ttc` files and mount them in the WASM preview under `/fonts/` (64 MB cap) alongside the Liberation bundle.

### Changed

- Module completion appends `;` when the call ends the statement (shape primitives such as `cube`, `sphere`, `cylinder`, and 2D primitives; user-defined modules; not CSG/transform builtins like `union`, `difference`, or `translate`)

### Fixed

- Renaming a `.scad` file from the Project View no longer throws an NPE in the rename handler
- Renaming an included `.scad` file updates `use`/`include` import paths in dependent files
- For/let loop variables resolve correctly in references, inspections, and completion (not only in completion lists)
- WASM preview vendor validation and stale `out/html` repair now require `openscad.fonts.js` alongside core WASM files

## [1.2.1]

### Added

- In-browser 3D preview via openscad-wasm (Manifold backend) with bundled Three.js viewer; preview works without a native OpenSCAD install
- Configurable preview scene background from the preview toolbar: **Clear Sky**, **Cornfield**, and **Dark Gradient**, using colors from the matching OpenSCAD render color schemes
- Preview loading status overlay with stage messages during WASM init and geometry rendering; **Initializing preview…** on first load and **Refreshing preview…** on later updates
- Collapsible **OpenSCAD output** console in the preview panel for `echo`, warnings, and errors (stdout/stderr from openscad-wasm)
- Console header **Error** / **Warning** badges and right-click **Copy** / **Select All** on preview output
- WASM preview enables OpenSCAD `--summary bounding-box` so render bounding box dimensions appear in the output console

### Changed

- Default preview background is **Dark Gradient** (was Clear Sky)
- Preview source collector bundles transitive `use`/`include` dependencies and resolves library-prefix imports (`BOSL2/`, `MCAD/`, etc.) from project roots and OpenSCAD library paths
- `syncOfficialOpenScadWasm` Gradle task copies artifacts from a sibling `openscad-wasm` build instead of shelling out to a script

### Fixed

- WASM preview bundling for BOSL2 projects (e.g. transitive `constants.scad` via `std.scad`, relative `lib/bosl2/` paths)
- Preview no longer recenters STL geometry at the origin; models keep OpenSCAD world coordinates so z = 0 aligns with the grid
- Expected preview failures (parse errors, non-zero OpenSCAD exit) no longer trigger the IDE internal error reporter; errors appear in the preview console instead
- Deprecated and scheduled-for-removal IntelliJ Platform API usages migrated to clear Plugin Verifier warnings
- Editor context menu: OpenSCAD submenu visible again in editor popup
- Open in OpenSCAD: launch detached so the background task completes immediately instead of waiting for the GUI to exit
- Parser: module calls that shadow builtin names (e.g. user-defined `floor()` module) parse and resolve correctly

## [1.1.0]

### Added

- Completion for `use` / `include` import paths and language keywords (imports, declarations, control flow, and literals)
- BOSL2 parser support: overridable builtin parsing, `each` / `assert` extensions, and test coverage so real library files (e.g. `skin.scad`) parse correctly

### Changed

- Marketplace display name: **OpenSCAD Support** (avoids conflict with the original plugin listing; Marketplace disallows "IntelliJ" in plugin names)
- Reunified `echo_element` with `arg_assignment_list` for consistent PSI and named-parameter resolution

### Fixed

- Optional JCEF dependency now uses a `config-file` descriptor for split-preview extensions

## [1.0.0]

Initial maintained fork release as `com.mjparme.idea-openscad` (replacing the unmaintained `com.javampire.idea-openscad` listing on JetBrains Marketplace).

### Added

- Rename support for modules, functions, and scoped variables
- Cross-file rename for file-scope variables via `include` and for modules/functions via `use`
- Semantic syntax highlighting for module, function, and variable names
- Semantic highlighting for module and function parameters
- Shift+F6 rename from declarations and call sites
- Unresolved reference inspection for modules, functions, and variables
- Module and function parameter resolution in bodies and at call sites
- Scoped completion and navigation for inner modules and functions
- IDE support for OpenSCAD special variables (e.g. `$preview`, `$fn`)
- `use` / `include`-aware completions with source file attribution
- Module completion: insert parentheses, optional named-argument fill with defaults, caret placed after parentheses
- Deferred global library completions until manual Ctrl+Space
- Live template context and bundled starter templates
- OpenSCAD syntax colors in Default, Darcula, and Islands Dark editor schemes

### Changed

- Forked from [ldenisey/idea-openscad](https://github.com/ldenisey/idea-openscad) for continued maintenance and JetBrains Marketplace release under plugin ID `com.mjparme.idea-openscad`
- Upgraded build to IntelliJ Platform 2026.2 and IntelliJ Platform Gradle Plugin 2.x (Kotlin DSL)
- Updated default syntax highlighting colors for identifiers, module names, function names, and variable names
- Preview auto-refresh on file save; preview toolbar action fixes
- Unified `echo` expression arguments with shared `arg_assignment_list` grammar
- CI: dedicated verify-plugin workflow, GitHub Actions cache and setup-java updates

### Fixed

- OpenSCAD split preview on modern IntelliJ platform (JCEF)
- JCEF preview detection under read lock on IntelliJ 2026.2 (defer check instead of caching false when the read lock is unavailable)
- Startup and preview initialization no longer perform blocking PSI/VFS work on the EDT at project load (ProjectActivity, deferred attach, write-safe preview site creation)
- Preview toolbar actions resolved via `UiDataProvider` with lazy `PSI_FILE` so toolbar updates do not block the UI thread
- Module rename from declarations and cross-file ambiguous names
- Cross-file rename for file-scope variables included from other files
- Shift+F6 rename handler chooser dialog conflict with platform inplace rename
- Relative `use` / `include` path resolution and imported symbol resolution
- Builtin module parameter resolution and positional argument completion
- Stub index crash when a declaration name is null
- Plugin Verifier failures by removing internal API usages

[Unreleased]: https://github.com/mjparme/idea-openscad/compare/v1.4.1...HEAD
[1.4.1]: https://github.com/mjparme/idea-openscad/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/mjparme/idea-openscad/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/mjparme/idea-openscad/compare/v1.2.1...v1.3.0
[1.2.1]: https://github.com/mjparme/idea-openscad/compare/v1.1.0...v1.2.1
[1.2.0]: https://github.com/mjparme/idea-openscad/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/mjparme/idea-openscad/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/mjparme/idea-openscad/commits/v1.0.0
