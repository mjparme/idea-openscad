import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { STLLoader } from "three/examples/jsm/loaders/STLLoader.js";

const NAME_MODEL = "SCAD_model";
const NAME_GRID = "GRID";
const NAME_GRID_LABELS = "GRID_LABELS";
const NAME_AXIS = "AXIS";
const NAME_CAMERA = "CAMERA";
const GRID_COLOR = 0x6a6a6a;
const GRID_MAJOR_COLOR = 0xa8a8a8;
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
  #previewGridLegend {
    position: fixed;
    left: 8px;
    bottom: 36px;
    z-index: 20;
    padding: 4px 8px;
    border-radius: 4px;
    font: 11px/1.3 -apple-system, system-ui, sans-serif;
    color: #e8e8e8;
    background: rgba(20, 20, 20, 0.72);
    border: 1px solid rgba(255, 255, 255, 0.12);
    pointer-events: none;
    user-select: none;
  }
  #previewGridLegend.hidden {
    display: none;
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

  const gridLegend = document.createElement("div");
  gridLegend.id = "previewGridLegend";
  gridLegend.className = "hidden";
  document.body.appendChild(gridLegend);

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
  layoutBottomOverlays();
  requestAnimationFrame(layoutBottomOverlays);
}

function layoutBottomOverlays() {
  const consoleRoot = document.getElementById("previewConsole");
  const bottom = consoleRoot
    ? Math.round(consoleRoot.getBoundingClientRect().height) + 8
    : 36;
  const offset = bottom + "px";
  const axis = document.getElementById("axisOrientation");
  if (axis) {
    axis.style.bottom = offset;
  }
  const legend = document.getElementById("previewGridLegend");
  if (legend) {
    legend.style.bottom = offset;
  }
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
  cornfield: { top: "#ffffe5", bottom: "#ffffe5" },
  metallic: { top: "#aaaaff", bottom: "#aaaaff" },
  sunset: { top: "#aa4444", bottom: "#aa4444" },
  starnight: { top: "#000000", bottom: "#000000" },
  beforedawn: { top: "#333333", bottom: "#333333" },
  nature: { top: "#fafafa", bottom: "#fafafa" },
  "daylight-gem": { top: "#f0f0f0", bottom: "#f0f0f0" },
  "nocturnal-gem": { top: "#0c0c0c", bottom: "#0c0c0c" },
  deepocean: { top: "#333333", bottom: "#333333" },
  solarized: { top: "#fdf6e3", bottom: "#fdf6e3" },
  tomorrow: { top: "#f8f8f8", bottom: "#f8f8f8" },
  "tomorrow-night": { top: "#1d1f21", bottom: "#1d1f21" },
  clearsky: { top: "#87ceeb", bottom: "#c9e9f6" },
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

function disposeObject3D(object) {
  object.traverse((child) => {
    if (child.geometry) {
      child.geometry.dispose();
    }
    const materials = child.material
      ? Array.isArray(child.material)
        ? child.material
        : [child.material]
      : [];
    for (const material of materials) {
      if (material.map) {
        material.map.dispose();
      }
      material.dispose();
    }
  });
}

function removeSceneObject(name) {
  const object = scene.getObjectByName(name);
  if (object) {
    scene.remove(object);
    disposeObject3D(object);
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

function niceGridStep(span) {
  const target = Math.max(span / 10, 1e-9);
  const exp = Math.pow(10, Math.floor(Math.log10(target)));
  const n = target / exp;
  if (n <= 1) {
    return exp;
  }
  if (n <= 2) {
    return 2 * exp;
  }
  if (n <= 5) {
    return 5 * exp;
  }
  return 10 * exp;
}

function snapHalfCellCount(count) {
  return Math.max(5, Math.ceil(count / 5) * 5);
}

function formatGridUnits(step) {
  if (!Number.isFinite(step)) {
    return String(step);
  }
  if (Math.abs(step - Math.round(step)) < 1e-9) {
    return String(Math.round(step));
  }
  return String(parseFloat(step.toPrecision(6)));
}

function updateGridLegend(enabled, step) {
  ensurePreviewUi();
  let legend = document.getElementById("previewGridLegend");
  if (!legend) {
    legend = document.createElement("div");
    legend.id = "previewGridLegend";
    document.body.appendChild(legend);
  }
  if (!enabled) {
    legend.classList.add("hidden");
    return;
  }
  legend.textContent = "Grid: " + formatGridUnits(step) + " units / cell";
  legend.classList.remove("hidden");
}

function getGridLayout() {
  const box = getModelBox();
  let minX = 0;
  let maxX = 10;
  let minY = 0;
  let maxY = 10;
  if (box) {
    minX = box.min.x;
    maxX = box.max.x;
    minY = box.min.y;
    maxY = box.max.y;
  }
  const span = Math.max(maxX - minX, maxY - minY, 1);
  let step = niceGridStep(span);
  const maxAbs = Math.max(
    Math.abs(minX),
    Math.abs(maxX),
    Math.abs(minY),
    Math.abs(maxY),
    step,
  );
  let halfCells = snapHalfCellCount((maxAbs + 2 * step) / step);
  let divisions = halfCells * 2;
  let size = divisions * step;
  if (divisions > 200) {
    step = niceGridStep(size / 10);
    halfCells = snapHalfCellCount((maxAbs + 2 * step) / step);
    divisions = halfCells * 2;
    size = divisions * step;
  }
  const majorEvery = divisions % 10 === 0 ? 10 : divisions % 5 === 0 ? 5 : 0;
  return { size, step, divisions, majorEvery };
}

function createGridTickLabel(text, x, y, z, height) {
  const canvas = document.createElement("canvas");
  canvas.width = 256;
  canvas.height = 64;
  const ctx = canvas.getContext("2d");
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.font = "bold 32px -apple-system, system-ui, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  const metrics = ctx.measureText(text);
  const padX = 16;
  const boxW = Math.min(canvas.width - 8, Math.max(48, metrics.width + padX * 2));
  const boxH = 44;
  const boxX = (canvas.width - boxW) / 2;
  const boxY = (canvas.height - boxH) / 2;
  ctx.fillStyle = "rgba(18, 18, 18, 0.72)";
  ctx.fillRect(boxX, boxY, boxW, boxH);
  ctx.fillStyle = "#f2f2f2";
  ctx.fillText(text, canvas.width / 2, canvas.height / 2 + 1);
  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  const sprite = new THREE.Sprite(
    new THREE.SpriteMaterial({
      map: texture,
      transparent: true,
      depthTest: true,
      depthWrite: false,
    }),
  );
  sprite.position.set(x, y, z);
  sprite.scale.set(height * (canvas.width / canvas.height), height, 1);
  sprite.renderOrder = 1;
  sprite.frustumCulled = false;
  return sprite;
}

let showGridLabelsEnabled = true;

function syncGridLabels(root, size, step, divisions, majorEvery) {
  const existing = root.getObjectByName(NAME_GRID_LABELS);
  if (existing) {
    root.remove(existing);
    disposeObject3D(existing);
  }
  if (!showGridLabelsEnabled || majorEvery <= 0) {
    return;
  }
  const labels = new THREE.Group();
  labels.name = NAME_GRID_LABELS;
  addGridEdgeLabels(labels, size, step, divisions, majorEvery);
  root.add(labels);
}

function addGridEdgeLabels(root, size, step, divisions, majorEvery) {
  if (majorEvery <= 0) {
    return;
  }
  const halfCells = divisions / 2;
  const half = size / 2;
  const out = step * 0.7;
  const lift = Math.max(step * 0.02, 0.02);
  const height = Math.max(step * 1.15, size * 0.028);
  for (let i = -halfCells; i <= halfCells; i += majorEvery) {
    const v = i * step;
    const label = formatGridUnits(v);
    root.add(createGridTickLabel(label, v, -half - out, lift, height));
    if (i !== -halfCells) {
      root.add(createGridTickLabel(label, -half - out, v, lift, height));
    }
  }
}

// Grid on XY plane (ground at Z=0 in OpenSCAD). Cell size is a 1-2-5-10 step
// in model units; extent covers origin and the model so geometry is not clipped.
function showGrid(enabled) {
  removeSceneObject(NAME_GRID);
  if (enabled) {
    const { size, step, divisions, majorEvery } = getGridLayout();
    updateGridLegend(true, step);
    const root = new THREE.Group();
    root.name = NAME_GRID;

    const minor = new THREE.GridHelper(
      size,
      divisions,
      new THREE.Color(GRID_COLOR),
      new THREE.Color(GRID_COLOR),
    );
    minor.rotation.x = Math.PI / 2;
    minor.receiveShadow = true;
    root.add(minor);

    if (majorEvery > 0 && divisions / majorEvery >= 2) {
      const major = new THREE.GridHelper(
        size,
        divisions / majorEvery,
        new THREE.Color(GRID_MAJOR_COLOR),
        new THREE.Color(GRID_MAJOR_COLOR),
      );
      major.rotation.x = Math.PI / 2;
      major.receiveShadow = true;
      root.add(major);
    }

    syncGridLabels(root, size, step, divisions, majorEvery);

    scene.add(root);
  } else {
    updateGridLegend(false);
  }
  window.cefQuery({ request: "showGrid=" + enabled });
}

function showGridLabels(enabled) {
  showGridLabelsEnabled = Boolean(enabled);
  sessionStorage.setItem("showGridLabels", String(showGridLabelsEnabled));
  const grid = scene.getObjectByName(NAME_GRID);
  if (grid) {
    const { size, step, divisions, majorEvery } = getGridLayout();
    syncGridLabels(grid, size, step, divisions, majorEvery);
  }
  window.cefQuery({ request: "showGridLabels=" + showGridLabelsEnabled });
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
  sessionStorage.setItem("showGridLabels", String(showGridLabelsEnabled));
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

  let isShowGridLabels = sessionStorage.getItem("showGridLabels");
  if (isShowGridLabels === null) {
    isShowGridLabels = true;
    sessionStorage.setItem("showGridLabels", isShowGridLabels.toString());
  } else {
    isShowGridLabels = isShowGridLabels === "true";
  }
  showGridLabels(isShowGridLabels);

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

const VIEW_CUBE_CSS_SIZE = 104;
const VIEW_CUBE_SCALE = 28;
const VIEW_CUBE_INV = new THREE.Quaternion();
const VIEW_CUBE_SCRATCH = new THREE.Vector3();
const VIEW_CUBE_FACES = [
  {
    name: "front",
    label: "FRONT",
    nx: 0,
    ny: -1,
    nz: 0,
    corners: [
      [-1, -1, -1],
      [1, -1, -1],
      [1, -1, 1],
      [-1, -1, 1],
    ],
  },
  {
    name: "back",
    label: "BACK",
    nx: 0,
    ny: 1,
    nz: 0,
    corners: [
      [1, 1, -1],
      [-1, 1, -1],
      [-1, 1, 1],
      [1, 1, 1],
    ],
  },
  {
    name: "right",
    label: "RIGHT",
    nx: 1,
    ny: 0,
    nz: 0,
    corners: [
      [1, -1, -1],
      [1, 1, -1],
      [1, 1, 1],
      [1, -1, 1],
    ],
  },
  {
    name: "left",
    label: "LEFT",
    nx: -1,
    ny: 0,
    nz: 0,
    corners: [
      [-1, 1, -1],
      [-1, -1, -1],
      [-1, -1, 1],
      [-1, 1, 1],
    ],
  },
  {
    name: "top",
    label: "TOP",
    nx: 0,
    ny: 0,
    nz: 1,
    corners: [
      [-1, -1, 1],
      [1, -1, 1],
      [1, 1, 1],
      [-1, 1, 1],
    ],
  },
  {
    name: "bottom",
    label: "BOTTOM",
    nx: 0,
    ny: 0,
    nz: -1,
    corners: [
      [-1, 1, -1],
      [1, 1, -1],
      [1, -1, -1],
      [-1, -1, -1],
    ],
  },
];

const viewCubeCanvas = document.createElement("canvas");
viewCubeCanvas.id = "viewCube";
viewCubeCanvas.title = "Click a face to set the view";
Object.assign(viewCubeCanvas.style, {
  position: "fixed",
  top: "8px",
  right: "8px",
  width: VIEW_CUBE_CSS_SIZE + "px",
  height: VIEW_CUBE_CSS_SIZE + "px",
  zIndex: "20",
  pointerEvents: "auto",
  cursor: "default",
});
document.body.appendChild(viewCubeCanvas);
const viewCubeCtx = viewCubeCanvas.getContext("2d");
/** @type {{ name: string, pts: { x: number, y: number }[], depth: number, area: number }[]} */
let viewCubeHitFaces = [];
let viewCubeHoverName = null;

function sizeViewCubeCanvas() {
  const dpr = Math.max(window.devicePixelRatio || 1, 1);
  viewCubeCanvas.width = Math.round(VIEW_CUBE_CSS_SIZE * dpr);
  viewCubeCanvas.height = Math.round(VIEW_CUBE_CSS_SIZE * dpr);
}

function projectViewCubePoint(x, y, z, cx, cy) {
  VIEW_CUBE_SCRATCH.set(x, y, z).applyQuaternion(VIEW_CUBE_INV);
  return {
    x: cx + VIEW_CUBE_SCRATCH.x * VIEW_CUBE_SCALE,
    y: cy - VIEW_CUBE_SCRATCH.y * VIEW_CUBE_SCALE,
    z: VIEW_CUBE_SCRATCH.z,
  };
}

function viewCubeQuadArea(pts) {
  let sum = 0;
  for (let i = 0; i < pts.length; i++) {
    const a = pts[i];
    const b = pts[(i + 1) % pts.length];
    sum += a.x * b.y - b.x * a.y;
  }
  return Math.abs(sum) / 2;
}

function pointInViewCubeQuad(px, py, pts) {
  let inside = false;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const xi = pts[i].x;
    const yi = pts[i].y;
    const xj = pts[j].x;
    const yj = pts[j].y;
    const intersect =
      yi > py !== yj > py && px < ((xj - xi) * (py - yi)) / (yj - yi + 1e-12) + xi;
    if (intersect) {
      inside = !inside;
    }
  }
  return inside;
}

function viewCubeLocalPoint(event) {
  const rect = viewCubeCanvas.getBoundingClientRect();
  const w = rect.width || VIEW_CUBE_CSS_SIZE;
  const h = rect.height || VIEW_CUBE_CSS_SIZE;
  return {
    x: ((event.clientX - rect.left) / w) * VIEW_CUBE_CSS_SIZE,
    y: ((event.clientY - rect.top) / h) * VIEW_CUBE_CSS_SIZE,
  };
}

function hitViewCubeFace(px, py) {
  for (let i = viewCubeHitFaces.length - 1; i >= 0; i--) {
    const face = viewCubeHitFaces[i];
    if (pointInViewCubeQuad(px, py, face.pts)) {
      return face;
    }
  }
  return null;
}

function renderViewCube() {
  if (!viewCubeCtx) {
    return;
  }
  const dpr = viewCubeCanvas.width / VIEW_CUBE_CSS_SIZE;
  const ctx = viewCubeCtx;
  const size = VIEW_CUBE_CSS_SIZE;
  const cx = size / 2;
  const cy = size / 2;

  camera.updateMatrixWorld();
  VIEW_CUBE_INV.copy(camera.quaternion).invert();

  const projected = [];
  for (const face of VIEW_CUBE_FACES) {
    VIEW_CUBE_SCRATCH.set(face.nx, face.ny, face.nz).applyQuaternion(VIEW_CUBE_INV);
    if (VIEW_CUBE_SCRATCH.z <= 0.02) {
      continue;
    }
    const pts = face.corners.map((c) =>
      projectViewCubePoint(c[0], c[1], c[2], cx, cy),
    );
    const depth = pts.reduce((s, p) => s + p.z, 0) / pts.length;
    projected.push({
      name: face.name,
      label: face.label,
      pts,
      depth,
      area: viewCubeQuadArea(pts),
    });
  }
  projected.sort((a, b) => a.depth - b.depth);
  viewCubeHitFaces = projected;

  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, size, size);

  ctx.beginPath();
  ctx.arc(cx, cy, size * 0.48, 0, Math.PI * 2);
  ctx.fillStyle = "rgba(18, 18, 18, 0.28)";
  ctx.fill();

  ctx.lineJoin = "round";
  ctx.font = "bold 10px -apple-system, system-ui, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";

  for (const face of projected) {
    ctx.beginPath();
    ctx.moveTo(face.pts[0].x, face.pts[0].y);
    for (let i = 1; i < face.pts.length; i++) {
      ctx.lineTo(face.pts[i].x, face.pts[i].y);
    }
    ctx.closePath();
    const hovered = face.name === viewCubeHoverName;
    ctx.fillStyle = hovered ? "rgba(210, 220, 230, 0.95)" : "rgba(168, 176, 184, 0.92)";
    ctx.fill();
    ctx.strokeStyle = "rgba(40, 44, 48, 0.9)";
    ctx.lineWidth = 1.25;
    ctx.stroke();

    if (face.area > 380) {
      const mx = face.pts.reduce((s, p) => s + p.x, 0) / face.pts.length;
      const my = face.pts.reduce((s, p) => s + p.y, 0) / face.pts.length;
      ctx.fillStyle = hovered ? "#1a1a1a" : "#2c2c2c";
      ctx.fillText(face.label, mx, my);
    }
  }
}

function createViewPresets() {
  sizeViewCubeCanvas();

  const stopOrbit = (event) => {
    event.preventDefault();
    event.stopPropagation();
  };

  viewCubeCanvas.addEventListener("pointerdown", stopOrbit);
  viewCubeCanvas.addEventListener("pointerup", stopOrbit);
  viewCubeCanvas.addEventListener("wheel", stopOrbit, { passive: false });
  viewCubeCanvas.addEventListener("contextmenu", stopOrbit);

  viewCubeCanvas.addEventListener("pointermove", (event) => {
    const { x, y } = viewCubeLocalPoint(event);
    const hit = hitViewCubeFace(x, y);
    const name = hit ? hit.name : null;
    viewCubeCanvas.style.cursor = name ? "pointer" : "default";
    if (name !== viewCubeHoverName) {
      viewCubeHoverName = name;
      renderViewCube();
    }
  });

  viewCubeCanvas.addEventListener("pointerleave", () => {
    viewCubeCanvas.style.cursor = "default";
    if (viewCubeHoverName) {
      viewCubeHoverName = null;
      renderViewCube();
    }
  });

  viewCubeCanvas.addEventListener("click", (event) => {
    stopOrbit(event);
    const { x, y } = viewCubeLocalPoint(event);
    const hit = hitViewCubeFace(x, y);
    if (hit) {
      setViewPreset(hit.name);
    }
  });
}

/** OpenSCAD-style corner triad: 2D canvas, not a second WebGL viewport. */
const AXIS_OVERLAY_CSS_SIZE = 120;
const AXIS_OVERLAY_AXES = [
  { name: "X", color: "#e53935", dir: new THREE.Vector3(1, 0, 0) },
  { name: "Y", color: "#43a047", dir: new THREE.Vector3(0, 1, 0) },
  { name: "Z", color: "#1e88e5", dir: new THREE.Vector3(0, 0, 1) },
];
const axisOverlayInvQuat = new THREE.Quaternion();
const axisOverlayScratch = new THREE.Vector3();

const axisOverlayCanvas = document.createElement("canvas");
axisOverlayCanvas.id = "axisOrientation";
axisOverlayCanvas.setAttribute("aria-hidden", "true");
Object.assign(axisOverlayCanvas.style, {
  position: "fixed",
  right: "8px",
  bottom: "36px",
  width: AXIS_OVERLAY_CSS_SIZE + "px",
  height: AXIS_OVERLAY_CSS_SIZE + "px",
  zIndex: "20",
  pointerEvents: "none",
});
document.body.appendChild(axisOverlayCanvas);
const axisOverlayCtx = axisOverlayCanvas.getContext("2d");

function sizeAxisOverlayCanvas() {
  const dpr = Math.max(window.devicePixelRatio || 1, 1);
  axisOverlayCanvas.width = Math.round(AXIS_OVERLAY_CSS_SIZE * dpr);
  axisOverlayCanvas.height = Math.round(AXIS_OVERLAY_CSS_SIZE * dpr);
}

function renderAxisOverlay() {
  if (!axisOverlayCtx) {
    return;
  }
  const dpr = axisOverlayCanvas.width / AXIS_OVERLAY_CSS_SIZE;
  const ctx = axisOverlayCtx;
  const size = AXIS_OVERLAY_CSS_SIZE;
  const cx = size / 2;
  const cy = size / 2;
  const tipR = 8;
  const axisLen = size * 0.38;

  camera.updateMatrixWorld();
  axisOverlayInvQuat.copy(camera.quaternion).invert();

  const projected = AXIS_OVERLAY_AXES.map((axis) => {
    axisOverlayScratch.copy(axis.dir).applyQuaternion(axisOverlayInvQuat);
    return {
      name: axis.name,
      color: axis.color,
      x: cx + axisOverlayScratch.x * axisLen,
      y: cy - axisOverlayScratch.y * axisLen,
      depth: axisOverlayScratch.z,
    };
  }).sort((a, b) => a.depth - b.depth);

  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, size, size);

  ctx.beginPath();
  ctx.arc(cx, cy, size * 0.46, 0, Math.PI * 2);
  ctx.fillStyle = "rgba(20, 20, 20, 0.45)";
  ctx.fill();
  ctx.strokeStyle = "rgba(255, 255, 255, 0.18)";
  ctx.lineWidth = 1;
  ctx.stroke();

  ctx.lineCap = "round";
  for (const axis of projected) {
    const dx = axis.x - cx;
    const dy = axis.y - cy;
    const dist = Math.hypot(dx, dy) || 1;
    const stop = Math.max(dist - tipR, 0);
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + (dx / dist) * stop, cy + (dy / dist) * stop);
    ctx.strokeStyle = axis.color;
    ctx.lineWidth = 3.5;
    ctx.stroke();
  }

  ctx.font = "bold 12px -apple-system, system-ui, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  for (const axis of projected) {
    ctx.beginPath();
    ctx.arc(axis.x, axis.y, tipR, 0, Math.PI * 2);
    ctx.fillStyle = axis.color;
    ctx.fill();
    ctx.strokeStyle = "rgba(255, 255, 255, 0.85)";
    ctx.lineWidth = 1.25;
    ctx.stroke();
    ctx.fillStyle = "#ffffff";
    ctx.fillText(axis.name, axis.x, axis.y + 0.5);
  }
}

sizeAxisOverlayCanvas();

// Render scene
function render() {
  renderer.render(scene, camera);
  renderAxisOverlay();
  renderViewCube();
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
  sizeAxisOverlayCanvas();
  sizeViewCubeCanvas();
  layoutBottomOverlays();
  render();
}

// Expose functions
window.showAxis = showAxis;
window.showGrid = showGrid;
window.showGridLabels = showGridLabels;
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
    userFonts: payload.userFonts ?? {},
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

// Controls
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.08;
controls.screenSpacePanning = true;

updateRendererSize();

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
