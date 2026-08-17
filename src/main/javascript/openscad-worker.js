import { createOpenSCAD } from "openscad-wasm";

/** @type {import("openscad-wasm").OpenSCADInstance | null} */
let openscad = null;
let rendering = false;
/** @type {{ generation: number, mainPath: string, files: Record<string, string> } | null} */
let pendingRequest = null;

function ensureDir(instance, filePath) {
  const parts = filePath.split("/").filter(Boolean);
  let current = "";
  for (let i = 0; i < parts.length - 1; i++) {
    current += "/" + parts[i];
    try {
      instance.FS.mkdir(current);
    } catch {
      // Directory already exists.
    }
  }
}

function unlinkIfExists(instance, path) {
  try {
    instance.FS.unlink(path);
  } catch {
    // Ignore missing files.
  }
}

function mountFiles(instance, files) {
  for (const [path, content] of Object.entries(files)) {
    ensureDir(instance, path);
    unlinkIfExists(instance, path);
    instance.FS.writeFile(path, content);
  }
}

async function renderNow(request) {
  rendering = true;
  const { generation, mainPath, files } = request;
  const outputPath = `/preview-${generation}.stl`;

  try {
    if (!openscad) {
      self.postMessage({ type: "status", message: "Loading OpenSCAD WASM..." });
      openscad = await createOpenSCAD({
        print: (text) => self.postMessage({ type: "log", message: text }),
        printErr: (text) => self.postMessage({ type: "log", message: text }),
      });
    }

    const instance = openscad.getInstance();
    mountFiles(instance, files);
    unlinkIfExists(instance, outputPath);

    const exitCode = instance.callMain([
      mainPath,
      "--enable=manifold",
      "-o",
      outputPath,
    ]);

    if (exitCode !== 0) {
      self.postMessage({
        type: "error",
        generation,
        message: `OpenSCAD exited with code ${exitCode}`,
      });
      return;
    }

    const stlBytes = instance.FS.readFile(outputPath);
    unlinkIfExists(instance, outputPath);

    self.postMessage(
      { type: "done", generation, stl: stlBytes.buffer },
      [stlBytes.buffer],
    );
  } catch (error) {
    self.postMessage({
      type: "error",
      generation,
      message: error instanceof Error ? error.message : String(error),
    });
  } finally {
    rendering = false;
    if (pendingRequest) {
      const next = pendingRequest;
      pendingRequest = null;
      renderNow(next);
    }
  }
}

self.onmessage = (event) => {
  const { type, generation, mainPath, files } = event.data;
  if (type !== "render") {
    return;
  }

  const request = { generation, mainPath, files };
  if (rendering) {
    pendingRequest = request;
    return;
  }
  renderNow(request);
};
