package com.javampire.openscad.references;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.psi.OpenSCADImportUtil;

public class OpenSCADResolverTest extends BasePlatformTestCase {

    public void testResolvesParentRelativeImportPath() {
        myFixture.addFileToProject("lib/cubes.scad", "module roundedCube() { cube(1); }");
        final PsiFile mainFile = myFixture.addFileToProject("fiber-arts/thread-holder/main.scad", """
                use <../../lib/cubes.scad>
                roundedCube();
                """);

        final var resolved = OpenSCADResolver.findModuleContentFile(mainFile, "../../lib/cubes.scad");
        assertEquals(1, resolved.size());
        assertEquals("cubes.scad", resolved.get(0).getName());
    }

    public void testImportUtilResolvesAfterConfigureFromTempProjectFile() {
        myFixture.addFileToProject("lib/cubes.scad", "module roundedCube() { cube(1); }");
        myFixture.addFileToProject("fiber-arts/thread-holder/main.scad", """
                use <../../lib/cubes.scad>
                roundedCube();
                """);
        myFixture.configureFromTempProjectFile("fiber-arts/thread-holder/main.scad");

        final var resolved = OpenSCADImportUtil.resolveImportFiles(myFixture.getFile(), "../../lib/cubes.scad");
        assertEquals(1, resolved.size());
        assertNotNull(OpenSCADImportUtil.findImportedModule(myFixture.getFile(), "roundedCube"));
    }
}
