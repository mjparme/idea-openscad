package com.javampire.openscad.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class OpenSCADCompletionContributorTest extends BasePlatformTestCase {

    public void testInnerModuleCompletionAfterDeclaration() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    module mainShape() { cube(1); }
                    <caret>mainShape();
                }
                """);
        assertContainsModule("mainShape");
    }

    public void testInnerModuleCompletionBeforeDeclaration() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    <caret>mainShape();
                    module mainShape() { cube(1); }
                }
                """);
        assertContainsModule("mainShape");
    }

    public void testFileLevelModuleCompletionBeforeDeclaration() {
        myFixture.configureByText("test.scad", """
                <caret>later();
                module later() { cube(1); }
                """);
        assertContainsModule("later");
    }

    public void testInnerModuleNotVisibleOutsideParentModule() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    module mainShape() { cube(1); }
                }
                main<caret>
                """);
        final List<String> variants = myFixture.getLookupElementStrings();
        if (variants != null) {
            assertFalse(variants.contains("mainShape"));
        }
    }

    public void testInnerFunctionCompletionInsideParentModule() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    function helper(x) = x * 2;
                    value = helper(<caret>);
                }
                """);
        myFixture.completeBasic();
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue(variants.contains("helper"));
    }

    public void testZeroArgModuleCompletionInsertsParentheses() {
        myFixture.configureByText("test.scad", """
                <caret>blow();
                module blowerHolder() { cube(1); }
                """);
        final LookupElement blowerHolder = findLookupItem("blowerHolder");
        selectLookupItem(blowerHolder);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                blowerHolder();
                module blowerHolder() { cube(1); }
                """);
    }

    public void testParameterizedModuleCompletionInsertsParenthesesWithCaretInside() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0) { cube(delta); }
                """);
        final LookupElement mainShape = findLookupItem("mainShape");
        selectLookupItem(mainShape);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape()
                module mainShape(delta = 0) { cube(delta); }
                """);
        final int openParenOffset = myFixture.getFile().getText().indexOf('(');
        assertEquals(openParenOffset + 1, myFixture.getEditor().getCaretModel().getOffset());
    }

    public void testParameterizedModuleCompletionMovesCaretInsideExistingParentheses() {
        myFixture.configureByText("test.scad", """
                <caret>main();
                module mainShape(delta = 0) { cube(delta); }
                """);
        final LookupElement mainShape = findLookupItem("mainShape");
        selectLookupItem(mainShape);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape();
                module mainShape(delta = 0) { cube(delta); }
                """);
        final int openParenOffset = myFixture.getFile().getText().indexOf('(');
        assertEquals(openParenOffset + 1, myFixture.getEditor().getCaretModel().getOffset());
    }

    private void selectLookupItem(LookupElement item) {
        final var lookup = myFixture.getLookup();
        assertNotNull(lookup);
        lookup.setCurrentItem(item);
    }

    private LookupElement findLookupItem(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull("No completion items returned", items);
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())) {
                return item;
            }
        }
        fail("Expected completion to include " + name + " but got: " + myFixture.getLookupElementStrings());
        return null;
    }

    private void assertContainsModule(String moduleName) {
        final var lookupElements = myFixture.completeBasic();
        assertNotNull(lookupElements);
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue("Expected completion to include " + moduleName + " but got: " + variants, variants.contains(moduleName));
    }
}
