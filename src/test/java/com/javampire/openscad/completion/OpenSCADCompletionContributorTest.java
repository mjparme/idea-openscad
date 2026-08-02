package com.javampire.openscad.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.settings.OpenSCADSettings;

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

    public void testParameterizedModuleShowsWithArgsCompletionItem() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0, width = 10) { cube(delta); }
                """);
        assertNotNull(findLookupItemWithFilledArgs("mainShape"));
        assertEquals(2, countLookupItems("mainShape"));
    }

    public void testWithArgsCompletionItemFillsNamedArguments() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0, width = 10) { cube(delta); }
                """);
        final LookupElement mainShapeWithArgs = findLookupItemWithFilledArgs("mainShape");
        selectLookupItem(mainShapeWithArgs);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape(delta = 0, width = 10)
                module mainShape(delta = 0, width = 10) { cube(delta); }
                """);
        final int deltaOffset = myFixture.getFile().getText().indexOf("delta = 0");
        assertEquals(deltaOffset + "delta = 0".length(), myFixture.getEditor().getCaretModel().getOffset());
    }

    public void testWithArgsCompletionItemUsesEmptyPlaceholderForParametersWithoutDefaults() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0, extra) { cube(delta); }
                """);
        final LookupElement mainShapeWithArgs = findLookupItemWithFilledArgs("mainShape");
        selectLookupItem(mainShapeWithArgs);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape(delta = 0, extra = )
                module mainShape(delta = 0, extra) { cube(delta); }
                """);
    }

    public void testWithArgsCompletionItemHiddenWhenSettingEnabled() {
        final OpenSCADSettings settings = OpenSCADSettings.getInstance();
        final boolean previous = settings.isFillNamedArgumentsOnModuleCompletion();
        settings.setFillNamedArgumentsOnModuleCompletion(true);
        try {
            myFixture.configureByText("test.scad", """
                    <caret>main
                    module mainShape(delta = 0) { cube(delta); }
                    """);
            assertEquals(1, countLookupItems("mainShape"));
        }
        finally {
            settings.setFillNamedArgumentsOnModuleCompletion(previous);
        }
    }

    public void testParameterizedModuleSettingFillsNamedArguments() {
        final OpenSCADSettings settings = OpenSCADSettings.getInstance();
        final boolean previous = settings.isFillNamedArgumentsOnModuleCompletion();
        settings.setFillNamedArgumentsOnModuleCompletion(true);
        try {
            myFixture.configureByText("test.scad", """
                    <caret>main
                    module mainShape(delta = 0) { cube(delta); }
                    """);
            final LookupElement mainShape = findLookupItem("mainShape");
            selectLookupItem(mainShape);
            myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
            myFixture.checkResult("""
                    mainShape(delta = 0)
                    module mainShape(delta = 0) { cube(delta); }
                    """);
        }
        finally {
            settings.setFillNamedArgumentsOnModuleCompletion(previous);
        }
    }

    private void selectLookupItem(LookupElement item) {
        final var lookup = myFixture.getLookup();
        assertNotNull(lookup);
        lookup.setCurrentItem(item);
    }

    private LookupElement findLookupItem(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull("No completion items returned", items);
        LookupElement fallback = null;
        for (LookupElement item : items) {
            if (!name.equals(item.getLookupString())) {
                continue;
            }
            if (!OpenSCADCompletionContributor.lookupElementFillsNamedArguments(item)) {
                return item;
            }
            if (fallback == null) {
                fallback = item;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        fail("Expected completion to include " + name + " but got: " + myFixture.getLookupElementStrings());
        return null;
    }

    private LookupElement findLookupItemWithFilledArgs(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull("No completion items returned", items);
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())
                    && OpenSCADCompletionContributor.lookupElementFillsNamedArguments(item)) {
                return item;
            }
        }
        return null;
    }

    private int countLookupItems(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull(items);
        int count = 0;
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())) {
                count++;
            }
        }
        return count;
    }

    private void assertContainsModule(String moduleName) {
        final var lookupElements = myFixture.completeBasic();
        assertNotNull(lookupElements);
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue("Expected completion to include " + moduleName + " but got: " + variants, variants.contains(moduleName));
    }
}
