package com.javampire.openscad.psi;

import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OpenSCADPsiImplUtilTest extends BasePlatformTestCase {

    public void testAccessibleModuleDeclarationsIncludeInnerModules() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    difference() {
                        mainShape();
                    }
                    module mainShape() { cube(1); }
                }
                """);
        final OpenSCADModuleDeclaration funnel = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADModuleDeclaration.class);
        assertNotNull(funnel);
        final OpenSCADModuleCallObj call = PsiTreeUtil.findChildOfType(funnel, OpenSCADModuleCallObj.class);
        assertNotNull(call);

        final var accessibleModules = OpenSCADPsiImplUtil.getAccessibleModuleDeclarations(call);
        assertEquals(1, accessibleModules.size());
        assertEquals("mainShape", accessibleModules.get(0).getName());
    }

    public void testAccessibleModuleDeclarationsIncludeFileLevelModulesAfterCaret() {
        myFixture.configureByText("test.scad", """
                module first() {}
                module second() {
                    first<caret>
                }
                module third() {}
                """);
        final var accessibleModules = OpenSCADPsiImplUtil.getAccessibleModuleDeclarations(myFixture.getFile().findElementAt(myFixture.getCaretOffset()));
        assertEquals(2, accessibleModules.size());
        assertEquals("first", accessibleModules.get(0).getName());
        assertEquals("third", accessibleModules.get(1).getName());
    }

    public void testAccessibleModuleDeclarationsIncludeFileLevelForwardReference() {
        myFixture.configureByText("test.scad", """
                <caret>later();
                module later() {}
                """);
        final var accessibleModules = OpenSCADPsiImplUtil.getAccessibleModuleDeclarations(myFixture.getFile().findElementAt(myFixture.getCaretOffset()));
        assertEquals(1, accessibleModules.size());
        assertEquals("later", accessibleModules.get(0).getName());
    }
}
