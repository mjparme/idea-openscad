package com.javampire.openscad.settings;

import junit.framework.TestCase;

public class OpenSCADSettingsTest extends TestCase {

    public void testNormalizeExecutablePathTrimsWhitespace() {
        assertEquals("/tmp/openscad", OpenSCADSettings.normalizeExecutablePath("  /tmp/openscad  "));
    }

    public void testNormalizeExecutablePathResolvesMacAppBundle() {
        final String resolved = OpenSCADSettings.normalizeExecutablePath("/Applications/OpenSCAD.app");
        if (new java.io.File("/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD").isFile()) {
            assertEquals("/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD", resolved);
        }
    }

    public void testNormalizeExecutablePathReturnsNullForBlank() {
        assertNull(OpenSCADSettings.normalizeExecutablePath(" "));
        assertNull(OpenSCADSettings.normalizeExecutablePath(null));
    }
}
