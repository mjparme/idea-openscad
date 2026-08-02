package com.javampire.openscad.psi;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.inspections.OpenSCADUnresolvedReferenceInspection;

import java.util.ArrayList;
import java.util.List;

public class OpenSCADParameterResolutionTest extends BasePlatformTestCase {

    public void testModuleParameterResolvesInsideBody() {
        myFixture.configureByText("test.scad", """
                module mainShape(delta = 0) {
                    cube(<caret>delta);
                }
                """);
        final OpenSCADVariableRefExpr variableRef = PsiTreeUtil.getParentOfType(
                myFixture.getFile().findElementAt(myFixture.getCaretOffset()),
                OpenSCADVariableRefExpr.class
        );
        assertNotNull(variableRef);
        assertEquals("delta", variableRef.getName());
        final PsiReference reference = variableRef.getReference();
        assertNotNull(reference);
        final var resolved = reference.resolve();
        assertTrue(resolved instanceof OpenSCADArgDeclaration);
        assertEquals("delta", ((OpenSCADArgDeclaration) resolved).getName());
    }

    public void testCallSiteParameterResolvesToCalleeDeclaration() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    difference() {
                        mainShape();
                        translate([0, 0, 0]) mainShape(delta = 1);
                    }
                    module mainShape(delta = 0) {
                        cube(delta);
                    }
                }
                """);
        final List<OpenSCADParameterReference> paramRefs = new ArrayList<>(
                PsiTreeUtil.findChildrenOfType(myFixture.getFile(), OpenSCADParameterReference.class));
        assertEquals(1, paramRefs.size());
        assertEquals("delta", paramRefs.get(0).getName());
        final PsiReference reference = paramRefs.get(0).getReference();
        assertNotNull(reference);
        final var resolved = reference.resolve();
        assertTrue(resolved instanceof OpenSCADArgDeclaration);
        assertEquals("delta", ((OpenSCADArgDeclaration) resolved).getName());
    }

    public void testModuleParameterDoesNotProduceUnresolvedInspection() {
        myFixture.enableInspections(OpenSCADUnresolvedReferenceInspection.class);
        myFixture.configureByText("test.scad", """
                module funnel() {
                    difference() {
                        mainShape();
                        translate([0, 0, 0]) mainShape(delta = blowerHolderWallThickness);
                    }
                    module mainShape(delta = 0) {
                        funnelInputLength = fanOpeningOuterLength - delta;
                        cube(delta);
                    }
                }
                fanOpeningOuterLength = 10;
                blowerHolderWallThickness = 2;
                """);
        assertFalse(myFixture.doHighlighting().stream()
                .filter(info -> "delta".equals(info.getText()) && info.getSeverity() == HighlightSeverity.ERROR)
                .anyMatch(info -> info.getDescription() != null && info.getDescription().contains("Cannot resolve")));
    }
}
