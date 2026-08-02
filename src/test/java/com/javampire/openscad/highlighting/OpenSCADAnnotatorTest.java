package com.javampire.openscad.highlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class OpenSCADAnnotatorTest extends BasePlatformTestCase {

    public void testModuleAndVariableNamesUseDistinctSemanticColors() {
        myFixture.configureByText("test.scad", """
                myVar = 10;
                module myMod() {}
                myMod();
                function myFunc(x) = myVar + x;
                """);

        final List<HighlightInfo> highlights = myFixture.doHighlighting();

        assertHighlightKey("myVar", OpenSCADSyntaxHighlighter.VARIABLE_NAME, highlights, 2);
        assertHighlightKey("myMod", OpenSCADSyntaxHighlighter.MODULE_NAME, highlights, 2);
        assertHighlightKey("myFunc", OpenSCADSyntaxHighlighter.FUNCTION_NAME, highlights, 1);
    }

    public void testModuleParameterUsesSameColorInDeclarationAndBody() {
        myFixture.configureByText("test.scad", """
                fanOpeningOuterLength = 10;
                module mainShape(delta = 0) {
                    funnelInputLength = fanOpeningOuterLength - delta;
                    cube(delta);
                }
                """);

        final List<HighlightInfo> highlights = myFixture.doHighlighting();

        assertHighlightKey("delta", OpenSCADSyntaxHighlighter.PARAMETER_NAME, highlights, 3);
        assertHighlightKey("fanOpeningOuterLength", OpenSCADSyntaxHighlighter.VARIABLE_NAME, highlights, 2);
        assertHighlightKey("funnelInputLength", OpenSCADSyntaxHighlighter.VARIABLE_NAME, highlights, 1);
    }

    public void testFunctionParameterUsesParameterColor() {
        myFixture.configureByText("test.scad", """
                function add(x, y) = x + y;
                """);

        final List<HighlightInfo> highlights = myFixture.doHighlighting();

        assertHighlightKey("x", OpenSCADSyntaxHighlighter.PARAMETER_NAME, highlights, 2);
        assertHighlightKey("y", OpenSCADSyntaxHighlighter.PARAMETER_NAME, highlights, 2);
    }

    private static void assertHighlightKey(String text,
                                           TextAttributesKey expectedKey,
                                           List<HighlightInfo> highlights,
                                           int expectedCount) {
        final long matches = highlights.stream()
                .filter(info -> text.equals(info.getText()))
                .filter(info -> expectedKey.equals(info.forcedTextAttributesKey))
                .count();
        assertEquals(
                "Unexpected highlight count for '" + text + "' with key " + expectedKey.getExternalName(),
                expectedCount,
                matches
        );
    }
}
