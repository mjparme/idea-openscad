/** @type {object | null} */
let openscadInstance = null;
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

/** @param {string[]} logs */
function summarizeRenderIssues(logs) {
  const text = logs.join("");
  const issues = [];

  if (/CGAL error/i.test(text)) {
    issues.push(
      "OpenSCAD CGAL error while evaluating geometry (often hull() or rotate_extrude). "
        + "The preview may be incomplete; native OpenSCAD may render this model correctly.",
    );
  }

  const errorMatch = text.match(/ERROR:\s*[^\n\r]+/);
  if (errorMatch && !issues.some((issue) => issue.includes(errorMatch[0]))) {
    issues.push(errorMatch[0].trim());
  }

  const warningMatch = text.match(/WARNING:\s*[^\n\r]+/);
  if (warningMatch) {
    issues.push(warningMatch[0].trim());
  }

  return issues;
}

async function getOpenSCADInstance(print, printErr) {
  if (openscadInstance) {
    return openscadInstance;
  }
  self.postMessage({ type: "status", message: "Initializing OpenSCAD WASM..." });
  const vendorDir = "vendor/openscad";
  const moduleFile = "openscad.js";
  const moduleUrl = new URL(`${vendorDir}/${moduleFile}`, self.location.href);
  const module = await import(/* webpackIgnore: true */ moduleUrl.href);
  const OpenSCAD = module.default;
  openscadInstance = await OpenSCAD({
    noInitialRun: true,
    // Keep the WASM runtime alive between exports so auto-refresh can call callMain again.
    noExitRuntime: true,
    print,
    printErr,
  });
  self.postMessage({ type: "wasmReady" });
  return openscadInstance;
}

function discardOpenSCADInstance() {
  openscadInstance = null;
}

async function renderNow(request) {
  rendering = true;
  const { generation, mainPath, files } = request;
  const outputPath = `/preview-${generation}.stl`;
  /** @type {string[]} */
  const renderLogs = [];

  const appendLog = (text, stream = "stdout") => {
    renderLogs.push(text);
    self.postMessage({ type: "log", message: text, stream });
  };

  try {
    const instance = await getOpenSCADInstance(
      (text) => appendLog(text, "stdout"),
      (text) => appendLog(text, "stderr"),
    );

    const fileCount = Object.keys(files).length;
    self.postMessage({
      type: "status",
      message:
        fileCount === 1
          ? "Mounting project file..."
          : `Mounting ${fileCount} project files...`,
    });
    mountFiles(instance, files);
    unlinkIfExists(instance, outputPath);

    self.postMessage({
      type: "status",
      message: "Rendering geometry (this may take a while)...",
    });
    const exitCode = instance.callMain([mainPath, "--backend=manifold", "-o", outputPath]);

    if (exitCode !== 0) {
      discardOpenSCADInstance();
      const issues = summarizeRenderIssues(renderLogs);
      const detail = issues.length ? issues.join(" ") : `OpenSCAD exited with code ${exitCode}`;
      self.postMessage({
        type: "error",
        generation,
        message: detail,
      });
      return;
    }

    const stlBytes = instance.FS.readFile(outputPath);
    unlinkIfExists(instance, outputPath);

    const warnings = summarizeRenderIssues(renderLogs);

    self.postMessage(
      { type: "done", generation, stl: stlBytes.buffer, warnings },
      [stlBytes.buffer],
    );
  } catch (error) {
    discardOpenSCADInstance();
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
