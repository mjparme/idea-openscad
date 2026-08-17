package com.javampire.openscad.editor;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

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

    public void testGetFileContentUsesDocumentText() {
        final var file = myFixture.addFileToProject("model.scad", "cube(1);");
        final var document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                .getDocument(file.getVirtualFile());
        assertNotNull(document);
        com.intellij.openapi.application.WriteAction.run(() -> document.setText("sphere(5);"));
        assertEquals("sphere(5);", OpenSCADPreviewSourceCollector.getFileContent(file));
    }
}
