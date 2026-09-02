<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# OpenSCAD Intellij plugin ChangeLog

## [Unreleased]

### Added

- WASM preview lazy-loads the openscad-wasm **Liberation** font bundle (~8 MB) when bundled sources use `text()` or call `textmetrics()` / `fontmetrics()`; includes Liberation Sans, Serif, and Mono (regular/bold/italic). Models without text skip the download. Experimental builtins also pass `--enable=textmetrics`.

### Fixed

- Renaming a `.scad` file from the Project View no longer throws an NPE in the rename handler
- Renaming an included `.scad` file updates `use`/`include` import paths in dependent files

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

## [2.4.0]

### Added

- [PR-80](https://github.com/ldenisey/idea-openscad/pull/80) Adding formatting of unary operators, thanks to [Bert Baron](https://github.com/bertbaron)
- [Issue-81](https://github.com/ldenisey/idea-openscad/issues/81) Keyboard shortcut to refresh preview
- [Issue-99](https://github.com/ldenisey/idea-openscad/issues/99) Auto-refresh preview on file save

### Changed

- [Issue-83](https://github.com/ldenisey/idea-openscad/issues/83) Maintain viewport on preview refresh
- [PR-85](https://github.com/ldenisey/idea-openscad/pull/85) Refactor Build GitHub action to move Jetbrains plugin verification into a dedicated workflow that will automatically
  compute versions to test in parallel
- [PR-86](https://github.com/ldenisey/idea-openscad/pull/86) Fix EAP incompatibilities : removal of third party library
- Unifying settings code example
- [PR-98](https://github.com/ldenisey/idea-openscad/pull/98) Migrate JS script to vanilla THREE.js

### Fixed

- Bump dependencies

## [2.3.3]

### Added

- [Issue-39](https://github.com/ldenisey/idea-openscad/issues/39) highlight customizer comments

### Changed

- [Issue-45](https://github.com/ldenisey/idea-openscad/issues/45) Preview should not be computed when editor is in text only view

### Fixed

- [Issue-46](https://github.com/ldenisey/idea-openscad/issues/46) Some keywords are parsed as identifiers
- [Issue-41](https://github.com/ldenisey/idea-openscad/issues/41) Show preview activation popup only on OpenSCAD projects
- Bump dependencies
- GitHub set-output action migration

## [2.3.2]

### Added

- [Issue-36](https://github.com/ldenisey/idea-openscad/issues/36) Add support for exponent operator

### Changed

- Export available formats will depend on OpenSCAD version
- Technical refactoring
- Documentation cleaning

## [2.3.1]

### Added

- Adding a notification to activate preview editor when OpenSCAD executable is already configured (plugin update use case)
- Adding "Open in OpenSCAD" and "Export as ..." actions in preview panel toolbar
- Adding "off", "amf", "3mf", "dxf", "csg" and "pdf" as possible export format in "Export as" actions

### Changed

- Preview are loaded asynchronously to avoid UI freezes during load and refresh
- Context menu "Render" changed to "Open in OpenSCAD" for more clarity

### Fixed

- [Issue-34](https://github.com/ldenisey/idea-openscad/issues/34) Fix preview temporary folder selection in IDE without compilers (Webstorm, ...)

## [2.3.0]

### Added

- Preview split panel. Available when OpenSCAD is installed on the computer and configured in Settings -> Languages & Frameworks -> OpenSCAD menu.
  The preview is automatically generated by exporting the model into a stl file and displayed with [viewstl](https://github.com/omrips/viewstl).

### Changed

- Update gradle wrapper to 7.5.1

### Fixed

- Fixed deprecated calls to Jetbrains API.

## [2.2.0]

### Added

- [Issue-34](https://github.com/ncsaba/idea-openscad/issues/34) 2019.05 features not fully supported
- [Issue-38](https://github.com/ncsaba/idea-openscad/issues/38) 2019.05 list comprehensions does not parse
- [Issue-91](https://github.com/ncsaba/idea-openscad/issues/91) "include" can be included in a block object
- [Issue-92](https://github.com/ncsaba/idea-openscad/issues/92) Code style: can disable indent in cascade transformations

### Changed

- Dependency updates, cleaning build configuration

### Fixed

- [Issue-97](https://github.com/ncsaba/idea-openscad/issues/97) & [PR-102](https://github.com/ncsaba/idea-openscad/pull/102) Limit documentation provider for language OpenSCAD
  only (from kadhonn)
- [Issue-80](https://github.com/ncsaba/idea-openscad/issues/80) & [Issue-89](https://github.com/ncsaba/idea-openscad/issues/89) Identifiers can start with digits

## [2.1.1]

### Fixed

- [Issue-71](https://github.com/ncsaba/idea-openscad/issues/71) & [Issue-74](https://github.com/ncsaba/idea-openscad/issues/74) Fix color identifier detection
- [Issue-77](https://github.com/ncsaba/idea-openscad/issues/77) Fix npe when invoking file contextual action menu

## [2.1.0]

### Changed

- "Generate" ... actions have been transformed into an "Export as ..." action that allow for target file path and type selection.

### Fix

- [Issue-59](https://github.com/ncsaba/idea-openscad/issues/59) NullPointerException In Intellij
- [Issue-62](https://github.com/ncsaba/idea-openscad/issues/62) Doesn't open app correctly if file path includes spaces

## [2.0.1]

### Changed

- Update GitHub actions and changelog format

### Fix

- Fix [Issue-56](https://github.com/ncsaba/idea-openscad/issues/56)

## [2.0.0]

### Added

- Add code formatter
- Add settings for OpenSCAD libraries and executable
- Add import reference
- Add editor context menu open OpenSCAD and generate actions
- Add completion for variables, modules and functions

### Changed

- Update compatibility version from 192.2549 to no limit

### Fix

- Fix deprecated calls

## [1.3.0]

### Added

- Added structure view
- Partial code navigation (modules/functions/variables without considering context)
- Documentation popups

### Changed

- Change version number to 1.3.0

### Fix

- Fix known grammar parsing issues

[Unreleased]: https://github.com/mjparme/idea-openscad/compare/v1.2.0...HEAD
[2.4.0]: https://github.com/mjparme/idea-openscad/compare/v2.3.3...v2.4.0
[2.3.3]: https://github.com/mjparme/idea-openscad/compare/v2.3.2...v2.3.3
[2.3.2]: https://github.com/mjparme/idea-openscad/compare/v2.3.1...v2.3.2
[2.3.1]: https://github.com/mjparme/idea-openscad/compare/v2.3.0...v2.3.1
[2.3.0]: https://github.com/mjparme/idea-openscad/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/mjparme/idea-openscad/compare/v2.1.1...v2.2.0
[2.1.1]: https://github.com/mjparme/idea-openscad/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/mjparme/idea-openscad/compare/v2.0.1...v2.1.0
[2.0.1]: https://github.com/mjparme/idea-openscad/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/mjparme/idea-openscad/compare/v1.3.0...v2.0.0
[1.3.0]: https://github.com/mjparme/idea-openscad/commits/v1.3.0
[1.2.0]: https://github.com/mjparme/idea-openscad/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/mjparme/idea-openscad/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/mjparme/idea-openscad/compare/v2.4.0...v1.0.0
