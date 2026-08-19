#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WASM_BUILD="${OPENSCAD_WASM_BUILD:-$REPO_ROOT/../openscad-wasm/build}"
VENDOR_DIR="$REPO_ROOT/src/main/javascript/vendor/openscad"

required=(
  "$WASM_BUILD/openscad.js"
  "$WASM_BUILD/openscad.wasm.js"
  "$WASM_BUILD/openscad.wasm"
)

for file in "${required[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing official OpenSCAD WASM artifact: $file" >&2
    echo "Build openscad-wasm first, e.g.:" >&2
    echo "  cd ../openscad-wasm && OPENSCAD_REF=master gmake all" >&2
    exit 1
  fi
done

mkdir -p "$VENDOR_DIR"
cp "${required[@]}" "$VENDOR_DIR/"
echo "Synced official OpenSCAD WASM into $VENDOR_DIR"
