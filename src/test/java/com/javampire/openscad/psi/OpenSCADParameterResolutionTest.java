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

    public void testBuiltinModuleNamedParametersResolve() {
        myFixture.configureByText("test.scad", "cube(size = [1, 1, 1], center = true);");
        final List<OpenSCADParameterReference> paramRefs = new ArrayList<>(
                PsiTreeUtil.findChildrenOfType(myFixture.getFile(), OpenSCADParameterReference.class));
        assertEquals(2, paramRefs.size());
        for (OpenSCADParameterReference paramRef : paramRefs) {
            final PsiReference reference = paramRef.getReference();
            assertNotNull(reference);
            final var resolved = reference.resolve();
            assertTrue("Failed to resolve parameter: " + paramRef.getName(), resolved instanceof OpenSCADArgDeclaration);
            assertEquals(paramRef.getName(), ((OpenSCADArgDeclaration) resolved).getName());
        }
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

    public void testForLoopVariableResolvesInsideBody() {
        myFixture.configureByText("test.scad", """
                module alignmentGrooves() {
                    for (grooveZ = [0:1:10]) {
                        translate([0, 0, <caret>grooveZ]) cylinder(d = 2, h = 1);
                    }
                }
                """);
        final OpenSCADVariableRefExpr variableRef = PsiTreeUtil.getParentOfType(
                myFixture.getFile().findElementAt(myFixture.getCaretOffset()),
                OpenSCADVariableRefExpr.class
        );
        assertNotNull(variableRef);
        assertEquals("grooveZ", variableRef.getName());
        final PsiReference reference = variableRef.getReference();
        assertNotNull(reference);
        final var resolved = reference.resolve();
        assertTrue(resolved instanceof OpenSCADFullArgDeclaration);
        assertEquals("grooveZ", ((OpenSCADFullArgDeclaration) resolved).getName());
    }

    public void testForLoopVariableDoesNotProduceUnresolvedInspection() {
        myFixture.enableInspections(OpenSCADUnresolvedReferenceInspection.class);
        myFixture.configureByText("test.scad", """
                module alignmentGrooves() {
                    for (grooveZ = [0:1:10]) {
                        translate([0, 0, grooveZ]) cylinder(d = 2, h = 1);
                    }
                }
                """);
        assertFalse(myFixture.doHighlighting().stream()
                .filter(info -> "grooveZ".equals(info.getText()) && info.getSeverity() == HighlightSeverity.ERROR)
                .anyMatch(info -> info.getDescription() != null && info.getDescription().contains("Cannot resolve")));
    }

    public void testNestedForLoopVariablesResolve() {
        myFixture.configureByText("test.scad", """
                for (x = [0:10])
                    for (y = [0:4])
                        translate([x, y, 0]) cube(1);
                """);
        final List<OpenSCADVariableRefExpr> refs = new ArrayList<>(
                PsiTreeUtil.findChildrenOfType(myFixture.getFile(), OpenSCADVariableRefExpr.class));
        assertEquals(2, refs.size());
        final OpenSCADVariableRefExpr xRef = refs.stream().filter(r -> "x".equals(r.getName())).findFirst().orElseThrow();
        final OpenSCADVariableRefExpr yRef = refs.stream().filter(r -> "y".equals(r.getName())).findFirst().orElseThrow();
        assertTrue(xRef.getReference().resolve() instanceof OpenSCADFullArgDeclaration);
        assertTrue(yRef.getReference().resolve() instanceof OpenSCADFullArgDeclaration);
    }

    public void testListComprehensionForVariableResolvesInBody() {
        myFixture.configureByText("test.scad", """
                list = [for (i = [0:2:5]) <caret>i * i];
                """);
        final OpenSCADVariableRefExpr variableRef = PsiTreeUtil.getParentOfType(
                myFixture.getFile().findElementAt(myFixture.getCaretOffset()),
                OpenSCADVariableRefExpr.class
        );
        assertNotNull(variableRef);
        assertEquals("i", variableRef.getName());
        assertEquals(1, OpenSCADPsiImplUtil.getAccessibleForElementBindings(variableRef).size());
        final PsiReference reference = variableRef.getReference();
        assertNotNull(reference);
        final var resolved = reference.resolve();
        assertTrue(resolved instanceof OpenSCADForBinding);
        assertEquals("i", ((OpenSCADForBinding) resolved).getName());
    }

    public void testListComprehensionForVariableDoesNotProduceUnresolvedInspection() {
        myFixture.enableInspections(OpenSCADUnresolvedReferenceInspection.class);
        myFixture.configureByText("test.scad", """
                output = [for (a = [0:len(input) - 1]) func(input[a])];
                input = [1, 2, 3];
                function func(x) = x;
                """);
        assertFalse(myFixture.doHighlighting().stream()
                .filter(info -> "a".equals(info.getText()) && info.getSeverity() == HighlightSeverity.ERROR)
                .anyMatch(info -> info.getDescription() != null && info.getDescription().contains("Cannot resolve")));
    }

    public void testNestedListComprehensionForVariablesResolve() {
        myFixture.configureByText("test.scad", """
                flat = [for (a = [0:2]) for (b = [0:2]) a == b ? 1 : 0];
                """);
        final List<OpenSCADVariableRefExpr> refs = new ArrayList<>(
                PsiTreeUtil.findChildrenOfType(myFixture.getFile(), OpenSCADVariableRefExpr.class));
        final OpenSCADVariableRefExpr aRef = refs.stream().filter(r -> "a".equals(r.getName())).findFirst().orElseThrow();
        final OpenSCADVariableRefExpr bRef = refs.stream().filter(r -> "b".equals(r.getName())).findFirst().orElseThrow();
        assertTrue(aRef.getReference().resolve() instanceof OpenSCADForBinding);
        assertTrue(bRef.getReference().resolve() instanceof OpenSCADForBinding);
    }

    public void testListComprehensionForVariableVisibleToLetBinding() {
        myFixture.configureByText("test.scad", """
                list = [for (a = [1:4]) let (b = a * a) [a, b]];
                """);
        final List<OpenSCADVariableRefExpr> refs = new ArrayList<>(
                PsiTreeUtil.findChildrenOfType(myFixture.getFile(), OpenSCADVariableRefExpr.class));
        final OpenSCADVariableRefExpr aInLet = refs.stream()
                .filter(r -> "a".equals(r.getName()))
                .filter(r -> PsiTreeUtil.getParentOfType(r, OpenSCADFullArgDeclaration.class) != null)
                .findFirst()
                .orElseThrow();
        assertTrue(aInLet.getReference().resolve() instanceof OpenSCADForBinding);
    }
}
