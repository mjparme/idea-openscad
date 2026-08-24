#!/usr/bin/env bash
# Quick smoke test for official openscad-wasm build artifacts.
set -euo pipefail

BUILD_DIR="${1:-../openscad-wasm/build}"
SCAD="${2:-/tmp/hull-test.scad}"
OUT="${3:-/tmp/hull-test.stl}"

cat > "$SCAD" <<'EOF'
$fn = 32;
hull() {
    sphere(10);
    translate([40, 0, 0]) sphere(10);
}
EOF

if [[ ! -f "$BUILD_DIR/openscad.js" ]]; then
  echo "Missing $BUILD_DIR/openscad.js" >&2
  exit 1
fi

deno run --allow-read --allow-write --allow-env --allow-ffi --allow-net <<DENO
const buildDir = Deno.args[0];
const scadPath = Deno.args[1];
const outPath = Deno.args[2];
const openscadUrl = new URL(\`file://\${buildDir}/openscad.js\`);
const { default: OpenSCAD } = await import(openscadUrl.href);
const scad = await Deno.readTextFile(scadPath);
const instance = await OpenSCAD({
  noInitialRun: true,
  noExitRuntime: true,
  print: (text) => console.log(text.trimEnd()),
  printErr: (text) => console.error(text.trimEnd()),
});
instance.FS.writeFile("/input.scad", scad);
const code = instance.callMain(["/input.scad", "--backend=manifold", "--summary", "bounding-box", "-o", "/output.stl"]);
if (code !== 0) {
  console.error("OpenSCAD exited with code", code);
  Deno.exit(code);
}
const stl = instance.FS.readFile("/output.stl");
await Deno.writeFile(outPath, stl);
console.log("Wrote", outPath, "(" + stl.byteLength + " bytes)");
DENO
"$BUILD_DIR" "$SCAD" "$OUT"
