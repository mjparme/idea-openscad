import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { STLLoader } from "three/examples/jsm/loaders/STLLoader.js";

const NAME_MODEL = "SCAD_model";
const NAME_GRID = "GRID";
const NAME_AXIS = "AXIS";
const NAME_CAMERA = "CAMERA";
const GRID_COLOR = 0x909090;
const MODEL_COLOR = 0xfef353;
const PREVIEW_PREFIX = "$preview=true;\n";
/** Bumped when viewer coords change — clears stale camera session keys. */
const VIEWER_CONFIG_VERSION = "z-up-v2";

if (typeof window.cefQuery !== "function") {
  window.cefQuery = console.log;
}

window.cefQuery({ request: "Reloading from scratch" });

let previewConsoleExpanded =
  sessionStorage.getItem("previewConsoleExpanded") === "true";
let previewConsoleLineCount = 0;
/** @type {"error" | "warning" | null} */
let previewConsoleState = null;

function ensurePreviewUi() {
  let status = document.getElementById("previewStatus");
  let consoleRoot = document.getElementById("previewConsole");
  if (status && consoleRoot) {
    return { status, consoleRoot };
  }

  const style = document.createElement("style");
  style.textContent = `
  #previewStatus {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    align-items: center;
    gap: 12px;
    max-width: min(420px, calc(100vw - 32px));
    padding: 14px 18px;
    border-radius: 8px;
    font: 13px/1.4 -apple-system, system-ui, sans-serif;
    color: #f0f0f0;
    background: rgba(28, 28, 28, 0.92);
    border: 1px solid #555;
    box-shadow: 0 8px 28px rgba(0, 0, 0, 0.4);
    z-index: 25;
    pointer-events: none;
    white-space: pre-wrap;
  }
  #previewStatus.hidden {
    display: none;
  }
  #previewStatus .previewSpinner {
    width: 18px;
    height: 18px;
    border: 2px solid rgba(255, 255, 255, 0.25);
    border-top-color: #87ceeb;
    border-radius: 50%;
    animation: previewSpin 0.8s linear infinite;
    flex-shrink: 0;
  }
  @keyframes previewSpin {
    to { transform: rotate(360deg); }
  }
  #previewConsole {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 30;
    display: flex;
    flex-direction: column;
    font: 11px/1.4 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    color: #d4d4d4;
    background: rgba(24, 24, 24, 0.97);
    border-top: 1px solid #444;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.35);
    pointer-events: auto;
  }
  #previewConsole.preview-console-error {
    border-top-color: #a04040;
  }
  #previewConsole.preview-console-warning {
    border-top-color: #a08030;
  }
  #previewConsole.collapsed #previewConsoleBody {
    display: none;
  }
  #previewConsoleHeader {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 10px;
    background: rgba(36, 36, 36, 0.98);
    border-bottom: 1px solid #3a3a3a;
    border-left: 3px solid transparent;
    cursor: pointer;
    user-select: none;
    font: 11px/1.4 -apple-system, system-ui, sans-serif;
    color: #bbb;
  }
  #previewConsole.preview-console-error #previewConsoleHeader {
    border-left-color: #c75050;
    background: rgba(48, 28, 28, 0.98);
  }
  #previewConsole.preview-console-warning #previewConsoleHeader {
    border-left-color: #c9a227;
    background: rgba(44, 38, 22, 0.98);
  }
  #previewConsoleHeader:hover {
    color: #e0e0e0;
    background: rgba(44, 44, 44, 0.98);
  }
  #previewConsoleTitle {
    font-weight: 600;
    color: #ddd;
  }
  .previewConsoleBadge {
    display: none;
    font-size: 10px;
    font-weight: 600;
    padding: 1px 6px;
    border-radius: 3px;
    line-height: 1.3;
  }
  #previewConsole.preview-console-error #previewConsoleBadgeError {
    display: inline;
    color: #f0a0a0;
    background: rgba(120, 40, 40, 0.85);
  }
  #previewConsole.preview-console-warning #previewConsoleBadgeWarning {
    display: inline;
    color: #e8d080;
    background: rgba(100, 80, 20, 0.85);
  }
  #previewConsoleCount {
    color: #888;
    font-size: 10px;
  }
  #previewConsoleToggle {
    margin-left: auto;
    color: #888;
    font-size: 10px;
  }
  #previewConsoleBody {
    max-height: 28vh;
    overflow-y: auto;
    padding: 6px 10px 8px;
    white-space: pre-wrap;
    word-break: break-word;
    user-select: text;
    cursor: text;
  }
  #previewConsoleBody:empty::before {
    content: "OpenSCAD output will appear here during preview.";
    color: #666;
    font-style: italic;
  }
  .previewLogLine {
    margin: 0 0 2px;
  }
  .previewLogLine.stderr {
    color: #f0a0a0;
  }
  .previewLogLine.stdout {
    color: #c8d8c8;
  }
  #previewConsoleContextMenu {
    position: fixed;
    z-index: 40;
    display: none;
    min-width: 120px;
    padding: 4px 0;
    border-radius: 4px;
    font: 12px/1.4 -apple-system, system-ui, sans-serif;
    color: #e0e0e0;
    background: rgba(40, 40, 40, 0.98);
    border: 1px solid #555;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  }
  #previewConsoleContextMenu.visible {
    display: block;
  }
  #previewConsoleContextMenu button {
    display: block;
    width: 100%;
    padding: 5px 14px;
    border: none;
    background: transparent;
    color: inherit;
    text-align: left;
    cursor: pointer;
    font: inherit;
  }
  #previewConsoleContextMenu button:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.08);
  }
  #previewConsoleContextMenu button:disabled {
    color: #666;
    cursor: default;
  }
  `;
  document.head.appendChild(style);

  status = document.createElement("div");
  status.id = "previewStatus";
  status.className = "hidden";
  status.innerHTML =
    '<div class="previewSpinner" aria-hidden="true"></div><span class="previewStatusText"></span>';
  document.body.appendChild(status);

  consoleRoot = document.createElement("div");
  consoleRoot.id = "previewConsole";
  consoleRoot.className = previewConsoleExpanded ? "" : "collapsed";
  consoleRoot.innerHTML =
    '<div id="previewConsoleHeader">'
    + '<span id="previewConsoleTitle">OpenSCAD output</span>'
    + '<span id="previewConsoleBadgeError" class="previewConsoleBadge error">Error</span>'
    + '<span id="previewConsoleBadgeWarning" class="previewConsoleBadge warning">Warning</span>'
    + '<span id="previewConsoleCount"></span>'
    + '<span id="previewConsoleToggle"></span>'
    + "</div>"
    + '<div id="previewConsoleBody"></div>';
  document.body.appendChild(consoleRoot);

  const header = consoleRoot.querySelector("#previewConsoleHeader");
  header.addEventListener("click", () => {
    setPreviewConsoleExpanded(!previewConsoleExpanded);
  });

  const body = consoleRoot.querySelector("#previewConsoleBody");
  setupPreviewConsoleContextMenu(body);

  updatePreviewConsoleChrome();

  return { status, consoleRoot };
}

let previewConsoleContextMenu = null;

function hidePreviewConsoleContextMenu() {
  if (previewConsoleContextMenu) {
    previewConsoleContextMenu.classList.remove("visible");
  }
}

function getPreviewConsolePlainText(body) {
  return Array.from(body.querySelectorAll(".previewLogLine"))
    .map((line) => line.textContent)
    .join("\n");
}

function copyTextToClipboard(text) {
  if (!text) {
    return false;
  }
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).catch(() => {
      copyTextToClipboardFallback(text);
    });
    return true;
  }
  return copyTextToClipboardFallback(text);
}

function copyTextToClipboardFallback(text) {
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.style.position = "fixed";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  let copied = false;
  try {
    copied = document.execCommand("copy");
  } catch {
    copied = false;
  }
  document.body.removeChild(textarea);
  return copied;
}

function setupPreviewConsoleContextMenu(body) {
  if (!previewConsoleContextMenu) {
    previewConsoleContextMenu = document.createElement("div");
    previewConsoleContextMenu.id = "previewConsoleContextMenu";
    previewConsoleContextMenu.innerHTML =
      '<button type="button" data-action="copy">Copy</button>'
      + '<button type="button" data-action="select-all">Select All</button>';
    document.body.appendChild(previewConsoleContextMenu);

    previewConsoleContextMenu.addEventListener("mousedown", (event) => {
      event.preventDefault();
    });
    previewConsoleContextMenu.addEventListener("click", (event) => {
      const button = event.target.closest("button[data-action]");
      if (!button || button.disabled) {
        return;
      }
      const consoleBody = document.getElementById("previewConsoleBody");
      if (!consoleBody) {
        return;
      }
      if (button.dataset.action === "copy") {
        const selected = window.getSelection().toString();
        const text = selected || getPreviewConsolePlainText(consoleBody);
        copyTextToClipboard(text);
      } else if (button.dataset.action === "select-all") {
        const range = document.createRange();
        range.selectNodeContents(consoleBody);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
      }
      hidePreviewConsoleContextMenu();
    });

    document.addEventListener("click", hidePreviewConsoleContextMenu);
    document.addEventListener("contextmenu", (event) => {
      if (
        previewConsoleContextMenu
        && !previewConsoleContextMenu.contains(event.target)
        && !body.contains(event.target)
      ) {
        hidePreviewConsoleContextMenu();
      }
    });
    window.addEventListener("blur", hidePreviewConsoleContextMenu);
  }

  body.addEventListener("contextmenu", (event) => {
    event.preventDefault();
    setPreviewConsoleExpanded(true);
    const selected = window.getSelection().toString();
    const hasContent = body.querySelector(".previewLogLine") != null;
    const copyButton = previewConsoleContextMenu.querySelector('[data-action="copy"]');
    const selectAllButton = previewConsoleContextMenu.querySelector('[data-action="select-all"]');
    copyButton.disabled = !hasContent && !selected;
    selectAllButton.disabled = !hasContent;

    previewConsoleContextMenu.style.left = event.clientX + "px";
    previewConsoleContextMenu.style.top = event.clientY + "px";
    previewConsoleContextMenu.classList.add("visible");

    const rect = previewConsoleContextMenu.getBoundingClientRect();
    if (rect.right > window.innerWidth) {
      previewConsoleContextMenu.style.left = Math.max(0, window.innerWidth - rect.width - 4) + "px";
    }
    if (rect.bottom > window.innerHeight) {
      previewConsoleContextMenu.style.top = Math.max(0, window.innerHeight - rect.height - 4) + "px";
    }
  });
}

function setPreviewConsoleState(state) {
  previewConsoleState = state;
  const consoleRoot = document.getElementById("previewConsole");
  if (!consoleRoot) {
    return;
  }
  consoleRoot.classList.remove("preview-console-error", "preview-console-warning");
  if (state === "error") {
    consoleRoot.classList.add("preview-console-error");
  } else if (state === "warning") {
    consoleRoot.classList.add("preview-console-warning");
  }
}

function updatePreviewConsoleChrome() {
  const ui = ensurePreviewUi();
  const countLabel = ui.consoleRoot.querySelector("#previewConsoleCount");
  const toggleLabel = ui.consoleRoot.querySelector("#previewConsoleToggle");
  countLabel.textContent =
    previewConsoleLineCount > 0 ? `(${previewConsoleLineCount})` : "";
  toggleLabel.textContent = previewConsoleExpanded ? "Hide" : "Show";
  ui.consoleRoot.classList.toggle("collapsed", !previewConsoleExpanded);
  setPreviewConsoleState(previewConsoleState);
}

function setPreviewConsoleExpanded(expanded) {
  previewConsoleExpanded = expanded;
  sessionStorage.setItem("previewConsoleExpanded", String(expanded));
  updatePreviewConsoleChrome();
}

function setPreviewStatus(message) {
  const { status } = ensurePreviewUi();
  status.querySelector(".previewStatusText").textContent = message;
  status.classList.remove("hidden");
  window.cefQuery({ request: "previewStatus=" + message });
}

function clearPreviewStatus() {
  const status = document.getElementById("previewStatus");
  if (status) {
    status.classList.add("hidden");
    status.querySelector(".previewStatusText").textContent = "";
  }
}

function reportPreviewIssue(kind, message) {
  const prefix = kind === "error" ? "previewError=" : "previewWarning=";
  window.cefQuery({ request: prefix + message });
  if (kind === "error" || previewConsoleState !== "error") {
    setPreviewConsoleState(kind);
  }
  setPreviewConsoleExpanded(true);
}

function clearPreviewConsole() {
  previewConsoleLineCount = 0;
  previewConsoleState = null;
  const body = document.getElementById("previewConsoleBody");
  if (body) {
    body.textContent = "";
  }
  updatePreviewConsoleChrome();
}

function appendPreviewLog(text, stream = "stdout") {
  const trimmed = String(text).replace(/\r?\n$/, "");
  if (!trimmed) {
    return;
  }
  const { consoleRoot } = ensurePreviewUi();
  const body = consoleRoot.querySelector("#previewConsoleBody");
  const line = document.createElement("div");
  line.className = "previewLogLine " + stream;
  line.textContent = trimmed;
  body.appendChild(line);
  body.scrollTop = body.scrollHeight;
  previewConsoleLineCount += 1;
  updatePreviewConsoleChrome();
  const encoded = encodeURIComponent(trimmed);
  window.cefQuery({
    request: "previewLog=" + stream + ":" + encoded,
  });
}

const PREVIEW_BACKGROUNDS = {
  clearsky: { top: "#87ceeb", bottom: "#c9e9f6" },
  cornfield: { top: "#ffffe5", bottom: "#ffffe5" },
  "dark-gradient": { top: "#2d2d2d", bottom: "#1a1a1a" },
};
const DEFAULT_PREVIEW_BACKGROUND = "dark-gradient";
let backgroundTexture = null;

function syncOpaqueBackgroundSurfaces(topHex) {
  document.documentElement.style.backgroundColor = topHex;
  document.body.style.backgroundColor = topHex;
  renderer.setClearColor(new THREE.Color(topHex), 1);
}

function applyPreviewBackground(id, syncToJava) {
  const scheme =
    PREVIEW_BACKGROUNDS[id] || PREVIEW_BACKGROUNDS[DEFAULT_PREVIEW_BACKGROUND];
  const resolvedId = PREVIEW_BACKGROUNDS[id]
    ? id
    : DEFAULT_PREVIEW_BACKGROUND;

  if (backgroundTexture) {
    backgroundTexture.dispose();
    backgroundTexture = null;
  }

  syncOpaqueBackgroundSurfaces(scheme.top);

  if (scheme.top === scheme.bottom) {
    scene.background = new THREE.Color(scheme.top);
  } else {
    backgroundTexture = makeGradientTexture(scheme.top, scheme.bottom);
    scene.background = backgroundTexture;
  }

  sessionStorage.setItem("previewBackground", resolvedId);
  if (syncToJava !== false) {
    window.cefQuery({ request: "previewBackground=" + resolvedId });
  }
  render();
}

function setPreviewBackground(id) {
  applyPreviewBackground(id, true);
}

function makeGradientTexture(topHex, bottomHex) {
  const canvas = document.createElement("canvas");
  canvas.width = 2;
  canvas.height = 256;
  const ctx = canvas.getContext("2d");
  const grad = ctx.createLinearGradient(0, 0, 0, 256);
  grad.addColorStop(0, topHex);
  grad.addColorStop(1, bottomHex);
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, 2, 256);
  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.magFilter = THREE.LinearFilter;
  return texture;
}

function getModelBox() {
  const mesh = scene.getObjectByName(NAME_MODEL);
  if (!mesh) {
    return null;
  }
  return new THREE.Box3().setFromObject(mesh);
}

function getModelBoxSize() {
  const mesh = scene.getObjectByName(NAME_MODEL);
  if (!mesh) {
    return new THREE.Vector3(100, 100, 100);
  }
  if (!mesh.boxSize) {
    const box3Size = new THREE.Vector3();
    new THREE.Box3().setFromObject(mesh).getSize(box3Size);
    mesh.boxSize = box3Size;
  }
  return mesh.boxSize;
}

function getModelSize() {
  const mesh = scene.getObjectByName(NAME_MODEL);
  if (!mesh) {
    return 100;
  }
  if (!mesh.size) {
    const modelBoxSize = getModelBoxSize();
    mesh.size = Math.max(modelBoxSize.x, modelBoxSize.y, modelBoxSize.z) * 1.5;
    if (mesh.size <= 0) {
      const height = isNaN(window.innerHeight)
        ? window.clientHeight
        : window.innerHeight;
      const width = isNaN(window.innerWidth)
        ? window.clientWidth
        : window.innerWidth;
      mesh.size = Math.min(height, width) * 0.8;
    }
  }
  return mesh.size;
}

function getViewDistance() {
  const box = getModelBox();
  if (!box) {
    return getModelSize();
  }
  const size = new THREE.Vector3();
  box.getSize(size);
  return Math.max(size.x, size.y, size.z, 1) * 2.2;
}

function getModelCenter() {
  const box = getModelBox();
  if (!box) {
    return new THREE.Vector3();
  }
  return box.getCenter(new THREE.Vector3());
}

function removeSceneObject(name) {
  const object = scene.getObjectByName(name);
  if (object) {
    scene.remove(object);
  }
}

// Axis (OpenSCAD Z-up — no extra rotation)
function showAxis(enabled) {
  removeSceneObject(NAME_AXIS);
  if (enabled) {
    const axisHelper = new THREE.AxesHelper(getModelSize() / 2);
    axisHelper.name = NAME_AXIS;
    scene.add(axisHelper);
  }
  window.cefQuery({ request: "showAxis=" + enabled });
}

// Grid on XY plane (ground at Z=0 in OpenSCAD)
function showGrid(enabled) {
  removeSceneObject(NAME_GRID);
  if (enabled) {
    const divisions = 10;
    const grid = new THREE.GridHelper(
      getModelSize(),
      divisions,
      new THREE.Color(GRID_COLOR),
      new THREE.Color(GRID_COLOR),
    );
    grid.name = NAME_GRID;
    grid.rotation.x = Math.PI / 2;
    grid.receiveShadow = true;
    scene.add(grid);
  }
  window.cefQuery({ request: "showGrid=" + enabled });
}

// Model color
function setModelColor(color) {
  const mesh = scene.getObjectByName(NAME_MODEL);
  if (!mesh) {
    return;
  }
  mesh.material.color.setHex(color);
  window.cefQuery({ request: "modelColor=0x" + color.toString(16) });
}

function saveConfiguration() {
  const mesh = scene.getObjectByName(NAME_MODEL);
  if (!mesh) {
    return;
  }
  sessionStorage.setItem("color", mesh.material.color.getHex().toString());
  sessionStorage.setItem(
    "showAxis",
    (typeof scene.getObjectByName(NAME_AXIS) === "object").toString(),
  );
  sessionStorage.setItem(
    "showGrid",
    (typeof scene.getObjectByName(NAME_GRID) === "object").toString(),
  );
  sessionStorage.setItem("previewBackground", sessionStorage.getItem("previewBackground") || DEFAULT_PREVIEW_BACKGROUND);
  sessionStorage.setItem("position", camera.position.toArray());
  sessionStorage.setItem("quaternion", camera.quaternion.toArray());
}

function migrateViewerSession() {
  if (sessionStorage.getItem("viewerConfigVersion") === VIEWER_CONFIG_VERSION) {
    return;
  }
  sessionStorage.setItem("viewerConfigVersion", VIEWER_CONFIG_VERSION);
  sessionStorage.removeItem("position");
  sessionStorage.removeItem("quaternion");
}

function isValidQuaternion(q) {
  const len = Math.sqrt(
    q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w,
  );
  return len > 0.001;
}

function loadConfiguration() {
  migrateViewerSession();
  window.cefQuery({ request: "Load configuration" });
  let color = sessionStorage.getItem("color");
  if (color === null) {
    color = MODEL_COLOR;
    sessionStorage.setItem("color", color.toString());
  } else {
    color = parseFloat(color);
  }
  setModelColor(color);

  let isShowAxis = sessionStorage.getItem("showAxis");
  if (isShowAxis === null) {
    isShowAxis = true;
    sessionStorage.setItem("showAxis", isShowAxis.toString());
  } else {
    isShowAxis = isShowAxis === "true";
  }
  showAxis(isShowAxis);

  let isShowGrid = sessionStorage.getItem("showGrid");
  if (isShowGrid === null) {
    isShowGrid = true;
    sessionStorage.setItem("showGrid", isShowGrid.toString());
  } else {
    isShowGrid = isShowGrid === "true";
  }
  showGrid(isShowGrid);

  let previewBackground = sessionStorage.getItem("previewBackground");
  if (previewBackground === null) {
    previewBackground = DEFAULT_PREVIEW_BACKGROUND;
    sessionStorage.setItem("previewBackground", previewBackground);
  }
  applyPreviewBackground(previewBackground, false);

  const savedPosition = sessionStorage.getItem("position");
  const savedQuaternion = sessionStorage.getItem("quaternion");
  let restoredCamera = false;

  if (savedPosition != null) {
    const parsed = savedPosition.split(",");
    if (parsed.length >= 3) {
      camera.position.set(
        parseFloat(parsed[0]),
        parseFloat(parsed[1]),
        parseFloat(parsed[2]),
      );
      restoredCamera = true;
    }
  }

  if (savedQuaternion != null) {
    const parsed = savedQuaternion.split(",");
    if (parsed.length >= 4) {
      const q = new THREE.Quaternion(
        parseFloat(parsed[0]),
        parseFloat(parsed[1]),
        parseFloat(parsed[2]),
        parseFloat(parsed[3]),
      );
      if (isValidQuaternion(q)) {
        camera.quaternion.copy(q);
        restoredCamera = true;
      }
    }
  }

  if (restoredCamera) {
    controls.target.copy(getModelCenter());
    controls.update();
  } else {
    frameModel();
  }
}

function frameModel() {
  const center = getModelCenter();
  const dist = getViewDistance();
  camera.position.set(
    center.x + dist * 0.55,
    center.y - dist * 0.75,
    center.z + dist * 0.45,
  );
  controls.target.copy(center);
  controls.update();
  render();
  window.cefQuery({ request: "resetCamera" });
}

function setViewPreset(view) {
  const center = getModelCenter();
  const dist = getViewDistance();
  const p = center.clone();
  switch (view) {
    case "top":
      p.z += dist;
      break;
    case "bottom":
      p.z -= dist;
      break;
    case "front":
      p.y -= dist;
      break;
    case "back":
      p.y += dist;
      break;
    case "left":
      p.x -= dist;
      break;
    case "right":
      p.x += dist;
      break;
    default:
      frameModel();
      return;
  }
  camera.position.copy(p);
  controls.target.copy(center);
  controls.update();
  render();
}

function createViewPresets() {
  const style = document.createElement("style");
  style.textContent = `
    #viewCube {
      position: fixed;
      top: 8px;
      right: 8px;
      display: grid;
      grid-template-columns: repeat(3, 28px);
      gap: 2px;
      z-index: 10;
      font-family: -apple-system, system-ui, sans-serif;
      font-size: 9px;
      user-select: none;
    }
    #viewCube .vc-btn {
      width: 28px;
      height: 22px;
      line-height: 22px;
      text-align: center;
      background: rgba(60, 63, 65, 0.92);
      border: 1px solid #505050;
      border-radius: 2px;
      color: #999;
      cursor: pointer;
    }
    #viewCube .vc-btn:hover {
      background: rgba(78, 82, 84, 0.95);
      color: #ddd;
    }
    #viewCube .vc-empty { visibility: hidden; }
  `;
  document.head.appendChild(style);

  const container = document.createElement("div");
  container.id = "viewCube";
  const layout = [
    ["", "top", ""],
    ["left", "front", "right"],
    ["", "bottom", "back"],
  ];
  for (const row of layout) {
    for (const view of row) {
      const cell = document.createElement("div");
      if (!view) {
        cell.className = "vc-empty";
      } else {
        cell.className = "vc-btn";
        cell.textContent =
          view === "bottom" ? "Btm" : view.charAt(0).toUpperCase() + view.slice(1);
        cell.title = view;
        cell.addEventListener("click", () => setViewPreset(view));
      }
      container.appendChild(cell);
    }
  }
  document.body.appendChild(container);
}

// Render scene
function render() {
  renderer.render(scene, camera);
}

function animate() {
  requestAnimationFrame(animate);
  controls.update();
  render();
}

function getViewportSize() {
  const width =
    window.innerWidth ||
    document.documentElement.clientWidth ||
    document.body.clientWidth ||
    800;
  const height =
    window.innerHeight ||
    document.documentElement.clientHeight ||
    document.body.clientHeight ||
    600;
  return {
    width: Math.max(width, 1),
    height: Math.max(height, 1),
  };
}

function updateRendererSize() {
  const { width, height } = getViewportSize();
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(width, height, false);
  render();
}

// Expose functions
window.showAxis = showAxis;
window.showGrid = showGrid;
window.setModelColor = setModelColor;
window.setPreviewBackground = setPreviewBackground;
window.saveConfiguration = saveConfiguration;
window.loadConfiguration = loadConfiguration;
window.resetCamera = frameModel;
window.frameModel = frameModel;
window.setViewPreset = setViewPreset;

function displayStlGeometry(geometry) {
  const existing = scene.getObjectByName(NAME_MODEL);
  if (existing) {
    scene.remove(existing);
    existing.geometry.dispose();
    existing.boxSize = null;
    existing.size = null;
  }

  geometry.computeVertexNormals();

  const mesh = new THREE.Mesh(geometry, material);
  mesh.name = NAME_MODEL;
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  scene.add(mesh);
  updateRendererSize();
  loadConfiguration();
}

const loader = new STLLoader();
let previewWorker = null;
let previewGeneration = 0;
let previewWasmReady = false;

function resetPreviewWorker() {
  if (previewWorker) {
    previewWorker.terminate();
    previewWorker = null;
  }
  previewWasmReady = false;
}

function getPreviewWorker() {
  if (!previewWorker) {
    previewWorker = new Worker(
      new URL("./openscad-worker.js", import.meta.url),
      { type: "module" },
    );
    previewWorker.onmessage = (event) => {
      const { type, generation, message, stl, warnings, stream } = event.data;
      if (type === "wasmReady") {
        previewWasmReady = true;
        return;
      }
      if (type === "status") {
        setPreviewStatus(message);
        return;
      }
      if (type === "log") {
        appendPreviewLog(message, stream || "stdout");
        return;
      }
      if (type === "error") {
        console.error(message);
        clearPreviewStatus();
        reportPreviewIssue("error", message);
        appendPreviewLog(message, "stderr");
        resetPreviewWorker();
        return;
      }
      if (type === "done") {
        if (generation !== previewGeneration) {
          return;
        }
        setPreviewStatus("Loading model into viewer...");
        const geometry = loader.parse(stl);
        displayStlGeometry(geometry);
        clearPreviewStatus();
        if (warnings && warnings.length) {
          reportPreviewIssue("warning", warnings.join("\n\n"));
          for (const warning of warnings) {
            appendPreviewLog(warning, "stderr");
          }
        }
        window.cefQuery({ request: "WASM preview rendered" });
      }
    };
    previewWorker.onerror = (error) => {
      console.error(error);
      clearPreviewStatus();
      const errorMessage = "WASM worker error: " + error.message;
      reportPreviewIssue("error", errorMessage);
      appendPreviewLog(errorMessage, "stderr");
      window.cefQuery({ request: errorMessage });
      resetPreviewWorker();
    };
  }
  return previewWorker;
}

window.renderPreview = function renderPreview(payload) {
  const files = { ...payload.files };
  const mainContent = files[payload.mainPath];
  if (mainContent != null) {
    files[payload.mainPath] = PREVIEW_PREFIX + mainContent;
  }

  previewGeneration += 1;
  const generation = previewGeneration;

  clearPreviewConsole();
  setPreviewStatus(
    previewWasmReady ? "Refreshing preview..." : "Initializing preview...",
  );
  window.cefQuery({ request: "Rendering preview with WebAssembly" });
  getPreviewWorker().postMessage({
    type: "render",
    generation,
    mainPath: payload.mainPath,
    files,
    enableTextMetrics: Boolean(payload.enableTextMetrics),
    loadPreviewFonts: Boolean(payload.loadPreviewFonts),
  });
};

// Scene
const scene = new THREE.Scene();

// Camera — OpenSCAD right-handed Z-up
const { width: viewportWidth, height: viewportHeight } = getViewportSize();
const camera = new THREE.PerspectiveCamera(
  45,
  viewportWidth / viewportHeight,
  0.01,
  100000,
);
camera.name = NAME_CAMERA;
camera.up.set(0, 0, 1);
camera.position.set(100, -120, 80);

// Lighting — ambient + key + fill (not a point light on the camera)
scene.add(new THREE.AmbientLight(0xffffff, 0.45));

const sunLight = new THREE.DirectionalLight(0xffffff, 0.85);
sunLight.position.set(150, -200, 400);
sunLight.castShadow = true;
sunLight.shadow.mapSize.width = 2048;
sunLight.shadow.mapSize.height = 2048;
sunLight.shadow.camera.near = 1;
sunLight.shadow.camera.far = 5000;
sunLight.shadow.camera.left = -500;
sunLight.shadow.camera.right = 500;
sunLight.shadow.camera.top = 500;
sunLight.shadow.camera.bottom = -500;
scene.add(sunLight);

const fillLight = new THREE.DirectionalLight(0x8899cc, 0.3);
fillLight.position.set(-200, 100, -100);
scene.add(fillLight);

// Opaque canvas — avoids JCEF/body bleed washing out background colors
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
renderer.setPixelRatio(window.devicePixelRatio);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.setClearColor(0x2d2d2d, 1);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.domElement.style.display = "block";
renderer.domElement.style.position = "fixed";
renderer.domElement.style.inset = "0";
renderer.domElement.style.width = "100%";
renderer.domElement.style.height = "100%";
document.body.appendChild(renderer.domElement);
updateRendererSize();

// Controls
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.08;
controls.screenSpacePanning = true;

// Material — smooth shading (STL normals recomputed in displayStlGeometry)
const material = new THREE.MeshStandardMaterial({
  color: MODEL_COLOR,
  metalness: 0,
  roughness: 0.35,
});

createViewPresets();

window.addEventListener("resize", updateRendererSize, false);
if (typeof ResizeObserver !== "undefined") {
  new ResizeObserver(() => updateRendererSize()).observe(document.body);
}
requestAnimationFrame(updateRendererSize);

applyPreviewBackground(
  sessionStorage.getItem("previewBackground") || DEFAULT_PREVIEW_BACKGROUND,
  false,
);

animate();
