![Workflow build](https://github.com/mjparme/idea-openscad/actions/workflows/build.yml/badge.svg)

<!-- Plugin description -->
# OpenSCAD Language Support for IntelliJ Platform

[OpenSCAD](https://openscad.org/index.html) language plugin for IntelliJ Platform IDEs (Idea, PyCharm, etc). It provides :

* Preview split panel, based on [OpenSCAD](https://openscad.org/index.html) rendering
* Syntax highlighting and semantic highlighting for modules, functions, variables, and parameters
* Code completion (built-in modules, project symbols, `use` / `include`, and global libraries)
* Code navigation and rename for modules, functions, and scoped variables (including cross-file `use` / `include`)
* Unresolved reference inspection
* Formatting
* Code folding support
* Structure views
* Library support
* Color picking
* Actions for opening OpenSCAD and exporting model
* Color scheme close to the built-in OpenSCAD editor
* Live templates
<!-- Plugin description end -->

## Fork

This is a maintained fork of [ldenisey/idea-openscad](https://github.com/ldenisey/idea-openscad) by Lucien Denisey.
The original plugin on JetBrains Marketplace (`com.javampire.idea-openscad`) is no longer actively maintained.
This fork uses plugin ID `com.mjparme.idea-openscad` and is published separately.

## Configuration

### OpenSCAD executable

[OpenSCAD](https://openscad.org/downloads.html) needs to be installed on your machine for the preview editor and the context menu actions.

The plugin will search for an OpenSCAD executable in standard installation paths at startup.

Go in *Settings* -> *Languages & Frameworks* -> *OpenSCAD* to manually set your installation path and activate/deactivate the preview editor.

![OpenSCAD settings: executable path, preview editor, and module completion options](docs/screenshots/settings-languages-frameworks-openscad.png)

On macOS, the default install location is usually:

```text
/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD
```

(MacPorts installs may use `/Applications/MacPorts/OpenSCAD.app/Contents/MacOS/OpenSCAD` instead.)

### Code completion

Completion behavior depends on where symbols come from:

* **Built-in OpenSCAD modules and functions** (e.g. `cube`, `translate`, `union`) — included when you type or invoke completion normally.
* **Symbols from `use` and `include` in your project** — included automatically, with tail text showing the source file.
* **Modules and functions from OpenSCAD global library paths** — **not** shown in the automatic completion popup. Press **Ctrl+Space** (or your IDE’s *Code Completion* shortcut) explicitly to load them. Scanning every `.scad` file under library paths is deferred for performance; the completion list may show a hint to press the shortcut for global libraries.

After an explicit completion invoke, modules from library folders (e.g. MCAD) appear with the library path in tail text:

![Module completion from OpenSCAD global library paths after Ctrl+Space](docs/screenshots/global-library-module-completion.png)

Module completion can insert parentheses and optionally fill named arguments with defaults (*Settings* -> *Languages & Frameworks* -> *OpenSCAD* — see below).

### Module completion

*Settings* -> *Languages & Frameworks* -> *OpenSCAD* -> **Fill named arguments on module completion**

| Setting | Completion list | On accept |
|--------|------------------|-----------|
| **Off** (default) | Each module with parameters appears twice: `myModule` and `myModule (with args)` | `myModule` inserts `myModule()` (or `myModule(arg = default)` for positional-first builtins). Choose `(with args)` to insert a filled call with named arguments and defaults. |
| **On** | Only one entry per module (no `(with args)` variant) | Always inserts a filled call — parentheses plus named arguments, using default values when the module defines them. |

With **Fill named arguments on module completion** off, modules from a `use` file show both entries (tail text indicates the source file):

![Module completion showing plain and (with args) variants from an included library](docs/screenshots/module-completion-with-args-variant.png)

Applies to built-in modules, project modules, and modules from `use` / `include` / global libraries.

### Global libraries

OpenSCAD library paths configured in OpenSCAD are added as IDE libraries at startup.
You can navigate to symbols in those libraries and complete them after an explicit **Ctrl+Space** (see *Code completion* above).

Project `use` / `include` files are resolved separately and appear in completion without that extra step.

### Formatting

The formatting options are located in *Settings* -> *Editor* -> *Code Style* -> *OpenSCAD*.

### OpenSCAD color scheme

The OpenSCAD color scheme can be loaded in *Settings* -> *Editor* -> *Color Scheme* -> *OpenSCAD* -> *Scheme* -> *OpenSCAD.Default*.

![OpenSCAD color scheme settings for syntax and semantic highlighting](docs/screenshots/color-scheme-settings.png)

### Shortcuts

You can add shortcuts in *Settings* -> *Keymap* -> *Plugins* -> *OpenSCAD Language Support*.

## Preview panel

The plugin split preview editor will allow you to modify your code and easily check its result in the IDE.

![Split preview editor with semantic highlighting for modules, functions, variables, and parameters](docs/screenshots/split-preview-semantic-highlighting.png)

The preview is done through an OpenSCAD STL file generation hence some information like colors are lost.

You can manually refresh the preview by clicking on the ![Refresh icon](/src/main/resources/com/javampire/openscad/icons/refresh.svg) button in the preview panel or in the editor context menu.
Alternatively, you can activate the auto refresh with the button ![Autorefresh icon](/src/main/resources/com/javampire/openscad/icons/autoRefresh.svg) which refresh the preview at every file save.
If your model is complex you can temporarily lower the [$fn variable](https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Other_Language_Features#.24fa.2C_.24fs_and_.24fn) to speed up the preview generation.

Temporary files are kept in a temporary folder (*out*, *temp*, *tmp* or *.tmp* folder depending on your IDE) at your project root.
If you are using a CVS (i.e. git), best is to ignore this folder.

## Context menu

When editing a `.scad` file, right-click in the editor or on the editor tab and open the **OpenSCAD** submenu:

* **Open in OpenSCAD** — launch OpenSCAD with this file
* **Export as…** — export via the OpenSCAD command line
* **Refresh Preview** — regenerate the preview (only when the split preview editor is open)

All three require a configured OpenSCAD executable.

## Known issues

### BOSL2 library parsing (navigation, completion, inspections)

Large [BOSL2](https://github.com/BelfrySCAD/BOSL2) files may not parse completely. Symptoms include unresolved references to modules that exist in an included library file (for example `path_sweep` in `skin.scad`), missing completions from `include` chains, and false positives from the unresolved-reference inspection.

This is a **parser** limitation, not a resolver bug: the include/`use` machinery works when the library file parses. BOSL2 uses OpenSCAD features our grammar does not fully support yet (builtin modules called as functions, extended `assert` forms, `each`, and related syntax). Parser error recovery can stop partway through a file, so declarations later in the file never enter the PSI tree.

A fix is planned by porting upstream work — see [ldenisey/idea-openscad#109](https://github.com/ldenisey/idea-openscad/pull/109) (*Modify the parser to handle the files from the BOSL2 library*).

## Issues and requests

Issues and requests are tracked in the [Issues tab](https://github.com/mjparme/idea-openscad/issues).

## How to contribute

It is a free and opened plugin. Any help for coding, testing and reviewing are welcome !
Have a look at [dedicated page](CONTRIBUTING.md).