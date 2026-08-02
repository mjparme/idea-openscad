package com.javampire.openscad.inspections;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class OpenSCADUnresolvedReferenceInspectionTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.enableInspections(OpenSCADUnresolvedReferenceInspection.class);
    }

    public void testUnresolvedModuleCall() {
        myFixture.configureByText("test.scad", "unknownMod();");
        assertTrue(hasErrorOn(myFixture.doHighlighting(), "unknownMod"));
    }

    public void testUnresolvedVariable() {
        myFixture.configureByText("test.scad", "y = unknownVariable + 1;");
        assertTrue(hasErrorOn(myFixture.doHighlighting(), "unknownVariable"));
    }

    public void testUnresolvedFunctionCall() {
        myFixture.configureByText("test.scad", "result = unknownFunc(1);");
        assertTrue(hasErrorOn(myFixture.doHighlighting(), "unknownFunc"));
    }

    public void testResolvedReferencesAreNotMarked() {
        myFixture.configureByText("test.scad", """
                myVar = 10;
                module myMod() { echo(myVar); }
                myMod();
                function myFunc(x) = myVar + x;
                """);
        final List<HighlightInfo> highlights = myFixture.doHighlighting();
        assertFalse(hasErrorOn(highlights, "myVar"));
        assertFalse(hasErrorOn(highlights, "myMod"));
        assertFalse(hasErrorOn(highlights, "myFunc"));
    }

    public void testBuiltinModuleIsNotMarked() {
        myFixture.configureByText("test.scad", "cube(10);");
        assertFalse(hasErrorOn(myFixture.doHighlighting(), "cube"));
    }

    public void testUnresolvedVariableInEcho() {
        myFixture.configureByText("test.scad", "echo(unknownEchoVar);");
        assertTrue(hasErrorOn(myFixture.doHighlighting(), "unknownEchoVar"));
    }

    public void testUnresolvedVariableInEchoExpr() {
        myFixture.configureByText("test.scad", "result = echo(unknownEchoVar) unknownEchoVar;");
        assertTrue(hasErrorOn(myFixture.doHighlighting(), "unknownEchoVar"));
    }

    public void testScopedVariableResolves() {
        myFixture.configureByText("test.scad", """
                module test() {
                    localVar = 5;
                    echo(localVar);
                }
                """);
        assertFalse(hasErrorOn(myFixture.doHighlighting(), "localVar"));
    }

    private static boolean hasErrorOn(List<HighlightInfo> highlights, String text) {
        return highlights.stream()
                .filter(info -> text.equals(info.getText()))
                .anyMatch(info -> info.getSeverity() == HighlightSeverity.ERROR);
    }
}
