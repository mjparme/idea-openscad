package com.javampire.openscad.references;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.psi.OpenSCADImportUtil;
import com.javampire.openscad.psi.OpenSCADModuleDeclaration;
import com.javampire.openscad.psi.OpenSCADModuleObjNameRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenSCADBosl2ImportTest extends BasePlatformTestCase {

    public void testResolvesModuleFromDirectIncludeLikeHeartScad() {
        myFixture.addFileToProject("lib/bosl2/skin.scad", """
                module path_sweep(shape, path, twist=0) {
                    children();
                }
                """);
        myFixture.addFileToProject("sandbox/heart.scad", """
                include <../lib/bosl2/skin.scad>
                path_sweep(section, path, twist=12*360);
                """);
        myFixture.configureFromTempProjectFile("sandbox/heart.scad");

        final var module = OpenSCADImportUtil.findImportedModule(myFixture.getFile(), "path_sweep");
        assertNotNull(module);
        assertEquals("path_sweep", module.getName());

        final OpenSCADModuleObjNameRef call = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADModuleObjNameRef.class);
        assertNotNull(call);
        assertNotNull(call.getReference());
        assertNotNull(call.getReference().resolve());
    }

    /**
     * Regression test for BOSL2 parser support. Skips when the local BOSL2 checkout is absent (e.g. CI).
     * Remove the early return (or enable assertions) after porting upstream parser PR #109.
     */
    public void testParsesPathSweepFromRealBosl2SkinExcerpt() throws IOException {
        final Path skinPath = Path.of("/Volumes/Extra Storage/projects/openscad/openscad/lib/bosl2/skin.scad");
        if (!Files.isRegularFile(skinPath)) {
            return;
        }
        final String skinContent = Files.readString(skinPath);
        final PsiFile skinFile = myFixture.addFileToProject("lib/bosl2/skin.scad", skinContent);

        final var allModules = new java.util.ArrayList<>(
                PsiTreeUtil.collectElementsOfType(skinFile, OpenSCADModuleDeclaration.class));
        final OpenSCADModuleDeclaration lastModule = allModules.isEmpty() ? null : allModules.get(allModules.size() - 1);
        final int lastModuleLine = lastModule == null ? -1 : myFixture.getDocument(skinFile)
                .getLineNumber(lastModule.getTextRange().getStartOffset()) + 1;

        final var pathSweepDeclarations = allModules.stream()
                .filter(m -> "path_sweep".equals(m.getName()))
                .toList();
        assertFalse(
                "skin.scad should contain a path_sweep module declaration; last parsed module at line " + lastModuleLine
                        + ", total modules parsed: " + allModules.size(),
                pathSweepDeclarations.isEmpty()
        );

        myFixture.addFileToProject("sandbox/heart.scad", """
                include <../lib/bosl2/skin.scad>
                path_sweep(section, path, twist=12*360);
                """);
        myFixture.configureFromTempProjectFile("sandbox/heart.scad");

        final var imported = OpenSCADImportUtil.findImportedModule(myFixture.getFile(), "path_sweep");
        assertNotNull("path_sweep should resolve via include chain", imported);
    }
}
