/// <reference types="./openscad.d.ts" />
let factory;
function loadFactory() {
    const url = new URL(`./openscad.wasm.js`, import.meta.url).href;
    return import(url).then((module) => module.default);
}
async function OpenSCAD(options) {
    if (!factory) {
        factory = loadFactory().catch((error) => {
            // Don't cache a rejected import, so a failed load can be retried.
            factory = undefined;
            throw error;
        });
    }
    const createModule = await factory;
    return await createModule({
        noInitialRun: true,
        locateFile: (path) => new URL(`./${path}`, import.meta.url).href,
        ...options,
    });
}
let buildInfo;
function getBuildInfo() {
    if (!buildInfo) {
        buildInfo = readBuildInfo().catch((error) => {
            // Don't cache a failure, so it can be retried.
            buildInfo = undefined;
            throw error;
        });
    }
    return buildInfo;
}
async function readBuildInfo() {
    const lines = [];
    const instance = await OpenSCAD({
        noInitialRun: true,
        print: (text) => lines.push(text),
        printErr: () => { },
    });
    instance.callMain(["--info"]);
    return lines;
}

export { OpenSCAD as default, getBuildInfo };
