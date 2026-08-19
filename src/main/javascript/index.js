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
const VIEWER_CONFIG_VERSION = "z-up-v1";

if (typeof window.cefQuery !== "function") {
  window.cefQuery = console.log;
}

window.cefQuery({ request: "Reloading from scratch" });

function ensurePreviewOverlay() {
  let overlay = document.getElementById("previewOverlay");
  if (overlay) {
    return overlay;
  }

  const style = document.createElement("style");
  style.textContent = `
    #previewOverlay {
      position: fixed;
      left: 12px;
      right: 12px;
      bottom: 12px;
      max-width: 640px;
      margin: 0 auto;
      padding: 10px 12px;
      border-radius: 6px;
      font: 12px/1.45 -apple-system, system-ui, sans-serif;
      color: #f5f5f5;
      background: rgba(30, 30, 30, 0.94);
      border: 1px solid #555;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
      z-index: 20;
      white-space: pre-wrap;
      pointer-events: none;
    }
    #previewOverlay.error {
      border-color: #c75050;
      background: rgba(48, 22, 22, 0.95);
    }
    #previewOverlay.warning {
      border-color: #c9a227;
      background: rgba(42, 36, 18, 0.95);
    }
    #previewOverlay.hidden {
      display: none;
    }
  `;
  document.head.appendChild(style);

  overlay = document.createElement("div");
  overlay.id = "previewOverlay";
  overlay.className = "hidden";
  document.body.appendChild(overlay);
  return overlay;
}

function hidePreviewOverlay() {
  const overlay = document.getElementById("previewOverlay");
  if (overlay) {
    overlay.className = "hidden";
    overlay.textContent = "";
  }
}

function showPreviewOverlay(kind, message) {
  const overlay = ensurePreviewOverlay();
  overlay.className = kind;
  overlay.textContent = message;
  const prefix = kind === "error" ? "previewError=" : "previewWarning=";
  window.cefQuery({ request: prefix + message });
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
  geometry.center();
  scene.add(mesh);
  updateRendererSize();
  loadConfiguration();
}

const loader = new STLLoader();
let previewWorker = null;
let previewGeneration = 0;

function resetPreviewWorker() {
  if (previewWorker) {
    previewWorker.terminate();
    previewWorker = null;
  }
}

function getPreviewWorker() {
  if (!previewWorker) {
    previewWorker = new Worker(
      new URL("./openscad-worker.js", import.meta.url),
      { type: "module" },
    );
    previewWorker.onmessage = (event) => {
      const { type, generation, message, stl, warnings } = event.data;
      if (type === "status") {
        hidePreviewOverlay();
        window.cefQuery({ request: message });
        return;
      }
      if (type === "log") {
        window.cefQuery({ request: message });
        return;
      }
      if (type === "error") {
        console.error(message);
        showPreviewOverlay("error", message);
        resetPreviewWorker();
        return;
      }
      if (type === "done") {
        if (generation !== previewGeneration) {
          return;
        }
        const geometry = loader.parse(stl);
        displayStlGeometry(geometry);
        if (warnings && warnings.length) {
          showPreviewOverlay("warning", warnings.join("\n\n"));
        } else {
          hidePreviewOverlay();
        }
        window.cefQuery({ request: "WASM preview rendered" });
      }
    };
    previewWorker.onerror = (error) => {
      console.error(error);
      window.cefQuery({ request: "WASM worker error: " + error.message });
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

  hidePreviewOverlay();
  window.cefQuery({ request: "Rendering preview with WebAssembly" });
  getPreviewWorker().postMessage({
    type: "render",
    generation,
    mainPath: payload.mainPath,
    files,
  });
};

// Scene — Blender-style gradient (matches Flexible / CAD viewers)
const scene = new THREE.Scene();
scene.background = makeGradientTexture("#2d2d2d", "#1a1a1a");

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

// Renderer — alpha helps JCEF embed the canvas correctly in the IDE panel
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setPixelRatio(window.devicePixelRatio);
renderer.setClearColor(0x000000, 0);
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

animate();
