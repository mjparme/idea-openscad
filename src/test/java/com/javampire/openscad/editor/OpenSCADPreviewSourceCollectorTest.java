package com.javampire.openscad.editor;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.settings.OpenSCADSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class OpenSCADPreviewSourceCollectorTest extends BasePlatformTestCase {

    public void testResolveImportVirtualPath() {
        assertEquals(
                "/work/models/lib/part.scad",
                OpenSCADPreviewSourceCollector.resolveImportVirtualPath("/work/models/main.scad", "lib/part.scad")
        );
        assertEquals(
                "/work/shared/util.scad",
                OpenSCADPreviewSourceCollector.resolveImportVirtualPath("/work/models/main.scad", "../shared/util.scad")
        );
    }

    public void testCollectMainFileAndInclude() {
        myFixture.addFileToProject("lib/part.scad", "module part() cube(1);");
        final var main = myFixture.addFileToProject("models/main.scad", """
                include <../lib/part.scad>
                part();
                """);
        final var sources = OpenSCADPreviewSourceCollector.collect(
                myFixture.getProject(),
                main.getVirtualFile()
        );
        assertNotNull(sources);
        assertEquals("/work/models/main.scad", sources.mainPath());
        assertTrue(sources.files().containsKey("/work/models/main.scad"));
        assertTrue(sources.files().containsKey("/work/lib/part.scad"));
    }

    public void testCollectRelativeUseImports() {
        myFixture.addFileToProject("lib/cubes.scad", "module taperedCube() cube(1);");
        myFixture.addFileToProject("lib/patterns.scad", "module cubes() cube(1);");
        myFixture.addFileToProject("lib/shapes.scad", "module fillet() cube(1);");
        final var main = myFixture.addFileToProject("bird-feeder/squareBirdFeeder.scad", """
                use <../lib/cubes.scad>
                use <../lib/patterns.scad>
                use <../lib/shapes.scad>
                taperedCube();
                """);
        final var sources = OpenSCADPreviewSourceCollector.collect(
                myFixture.getProject(),
                main.getVirtualFile()
        );
        assertNotNull(sources);
        assertEquals("/work/bird-feeder/squareBirdFeeder.scad", sources.mainPath());
        assertTrue(sources.files().containsKey("/work/lib/cubes.scad"));
        assertTrue(sources.files().containsKey("/work/lib/patterns.scad"));
        assertTrue(sources.files().containsKey("/work/lib/shapes.scad"));
    }

    public void testCollectBoslLibraryImportPaths() {
        myFixture.addFileToProject("lib/bosl/math.scad", "function noop() = 0;");
        myFixture.addFileToProject("lib/bosl/beziers.scad", """
                use <BOSL/math.scad>
                function bezier() = noop();
                """);
        final var main = myFixture.addFileToProject("models/main.scad", """
                use <../lib/bosl/beziers.scad>
                bezier();
                """);
        final var sources = OpenSCADPreviewSourceCollector.collect(
                myFixture.getProject(),
                main.getVirtualFile()
        );
        assertNotNull(sources);
        assertTrue(sources.files().containsKey("/work/lib/bosl/beziers.scad"));
        assertTrue(sources.files().containsKey("/work/BOSL/math.scad"));
        assertEquals(
                "/work/BOSL/math.scad",
                OpenSCADPreviewSourceCollector.resolveImportVirtualPath("/work/lib/bosl/beziers.scad", "BOSL/math.scad")
        );
    }

    public void testCollectBosl2StdTransitiveIncludes() {
        myFixture.addFileToProject("lib/bosl2/constants.scad", "CENTER = [0, 0, 0];");
        myFixture.addFileToProject("lib/bosl2/std.scad", """
                include <constants.scad>
                """);
        final var main = myFixture.addFileToProject("clock/moon-clock/moonClock.scad", """
                include <../../lib/bosl2/std.scad>
                """);
        final var sources = OpenSCADPreviewSourceCollector.collect(
                myFixture.getProject(),
                main.getVirtualFile()
        );
        assertNotNull(sources);
        assertTrue(sources.files().containsKey("/work/lib/bosl2/std.scad"));
        assertTrue(sources.files().containsKey("/work/lib/bosl2/constants.scad"));
    }

    public void testResolveImportVirtualPathLibraryPrefix() {
        assertEquals(
                "/work/BOSL2/std.scad",
                OpenSCADPreviewSourceCollector.resolveImportVirtualPath("/work/clock/moon-clock/moonClock.scad", "BOSL2/std.scad")
        );
        assertEquals(
                "/work/MCAD/util.scad",
                OpenSCADPreviewSourceCollector.resolveImportVirtualPath("/work/models/main.scad", "MCAD/util.scad")
        );
        assertEquals(
                "/work/models/lib/part.scad",
                OpenSCADPreviewSourceCollector.resolveImportVirtualPath("/work/models/main.scad", "lib/part.scad")
        );
    }

    public void testUsesLibrarySearchPath() {
        assertTrue(OpenSCADPreviewSourceCollector.usesLibrarySearchPath("BOSL2/std.scad"));
        assertTrue(OpenSCADPreviewSourceCollector.usesLibrarySearchPath("MCAD/foo.scad"));
        assertFalse(OpenSCADPreviewSourceCollector.usesLibrarySearchPath("../lib/bosl2/std.scad"));
        assertFalse(OpenSCADPreviewSourceCollector.usesLibrarySearchPath("constants.scad"));
        assertFalse(OpenSCADPreviewSourceCollector.usesLibrarySearchPath("lib/bosl2/std.scad"));
    }

    public void testCollectBosl2LibraryPrefixImport() {
        myFixture.addFileToProject("BOSL2/constants.scad", "CENTER = [0, 0, 0];");
        myFixture.addFileToProject("BOSL2/std.scad", """
                include <constants.scad>
                """);
        final var main = myFixture.addFileToProject("models/main.scad", """
                include <BOSL2/std.scad>
                """);
        final var sources = OpenSCADPreviewSourceCollector.collect(
                myFixture.getProject(),
                main.getVirtualFile()
        );
        assertNotNull(sources);
        assertTrue(sources.files().containsKey("/work/BOSL2/std.scad"));
        assertTrue(sources.files().containsKey("/work/BOSL2/constants.scad"));
    }

    public void testCollectImportPathsFromText() {
        final var paths = OpenSCADPreviewSourceCollector.collectImportPathsFromText("""
                use <../lib/cubes.scad>
                include <config.scad>
                """);
        assertEquals(2, paths.size());
        assertEquals("../lib/cubes.scad", paths.get(0));
        assertEquals("config.scad", paths.get(1));
    }

    public void testRequiresTextMetricsDetectsBuiltinCalls() {
        assertFalse(OpenSCADPreviewSourceCollector.requiresTextMetrics("cube(1);"));
        assertFalse(OpenSCADPreviewSourceCollector.requiresTextMetrics(
                "linear_extrude(height = 1) text(text = \"B\", font = \"Liberation Sans\");"
        ));
        assertTrue(OpenSCADPreviewSourceCollector.requiresTextMetrics("size = textmetrics(\"Hi\");"));
        assertTrue(OpenSCADPreviewSourceCollector.requiresTextMetrics("metrics = fontmetrics(\"Liberation Sans\");"));
    }

    public void testRequiresPreviewFontsDetectsTextModule() {
        assertFalse(OpenSCADPreviewSourceCollector.requiresPreviewFonts("cube(1);"));
        assertTrue(OpenSCADPreviewSourceCollector.requiresPreviewFonts(
                "linear_extrude(height = 1) text(text = \"B\", font = \"Liberation Sans\");"
        ));
        assertTrue(OpenSCADPreviewSourceCollector.requiresPreviewFonts("size = textmetrics(\"Hi\");"));
    }

    public void testCollectLoadsPreviewFontsForTextModule() {
        final var main = myFixture.addFileToProject("models/letter.scad", """
                font = "Liberation Sans";
                linear_extrude(height = 1) text(text = "B", font = font);
                """);
        final var sources = OpenSCADPreviewSourceCollector.collect(
                myFixture.getProject(),
                main.getVirtualFile()
        );
        assertNotNull(sources);
        assertTrue(sources.loadPreviewFonts());
        assertFalse(sources.enableTextMetrics());
    }

    public void testCollectEnablesTextMetricsWhenIncludedFileUsesBuiltin() {
        myFixture.addFileToProject("lib/metrics.scad", "function width() = textmetrics(\"A\").size[0];");
        final var main = myFixture.addFileToProject("models/main.scad", """
                include <../lib/metrics.scad>
                cube(width());
                """);
        final var sources = OpenSCADPreviewSourceCollector.collect(
                myFixture.getProject(),
                main.getVirtualFile()
        );
        assertNotNull(sources);
        assertTrue(sources.loadPreviewFonts());
        assertTrue(sources.enableTextMetrics());
    }

    public void testCollectLoadsUserFontsWhenConfigured() throws IOException {
        final Path fontDirectory = Files.createTempDirectory("preview-fonts-config");
        try {
            Files.write(fontDirectory.resolve("Custom.ttf"), new byte[]{7, 8, 9});
            OpenSCADSettings.getInstance().setPreviewFontDirectories(List.of(fontDirectory.toString()));

            final var main = myFixture.addFileToProject("models/letter.scad", """
                    linear_extrude(height = 1) text(text = "B", font = "Custom");
                    """);
            final var sources = OpenSCADPreviewSourceCollector.collect(
                    myFixture.getProject(),
                    main.getVirtualFile()
            );

            assertNotNull(sources);
            assertTrue(sources.loadPreviewFonts());
            assertEquals(1, sources.userFonts().size());
            assertTrue(Arrays.equals(new byte[]{7, 8, 9}, sources.userFonts().get("/fonts/Custom.ttf")));
        }
        finally {
            OpenSCADSettings.getInstance().setPreviewFontDirectories(List.of());
            com.intellij.openapi.util.io.FileUtil.delete(fontDirectory);
        }
    }

    public void testCollectSkipsUserFontsWhenTextNotUsed() throws IOException {
        final Path fontDirectory = Files.createTempDirectory("preview-fonts-unused");
        try {
            Files.write(fontDirectory.resolve("Custom.ttf"), new byte[]{1});
            OpenSCADSettings.getInstance().setPreviewFontDirectories(List.of(fontDirectory.toString()));

            final var main = myFixture.addFileToProject("models/plain.scad", "cube(1);");
            final var sources = OpenSCADPreviewSourceCollector.collect(
                    myFixture.getProject(),
                    main.getVirtualFile()
            );

            assertNotNull(sources);
            assertFalse(sources.loadPreviewFonts());
            assertTrue(sources.userFonts().isEmpty());
        }
        finally {
            OpenSCADSettings.getInstance().setPreviewFontDirectories(List.of());
            com.intellij.openapi.util.io.FileUtil.delete(fontDirectory);
        }
    }

    public void testGetFileContentUsesDocumentText() {
        final var file = myFixture.addFileToProject("model.scad", "cube(1);");
        final var document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                .getDocument(file.getVirtualFile());
        assertNotNull(document);
        com.intellij.openapi.application.WriteAction.run(() -> document.setText("sphere(5);"));
        assertEquals("sphere(5);", OpenSCADPreviewSourceCollector.getFileContent(file));
    }
}
