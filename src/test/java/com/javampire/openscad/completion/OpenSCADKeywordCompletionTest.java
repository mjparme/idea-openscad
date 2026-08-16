package com.javampire.openscad.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OpenSCADKeywordCompletionTest extends BasePlatformTestCase {

    public void testIncludeKeywordAtFileStart() {
        myFixture.configureByText("main.scad", "<caret>");
        assertContainsKeyword("include");
    }

    public void testUseKeywordAtFileStart() {
        myFixture.configureByText("main.scad", "u<caret>");
        assertContainsKeyword("use");
    }

    public void testModuleKeywordAtFileStart() {
        myFixture.configureByText("main.scad", "mod<caret>");
        assertContainsKeyword("module");
    }

    public void testFunctionKeywordAtFileStart() {
        myFixture.configureByText("main.scad", "<caret>");
        assertContainsKeyword("function");
    }

    public void testLiteralKeywordsInExpression() {
        myFixture.configureByText("main.scad", "x = tr<caret>");
        assertContainsKeyword("true");
    }

    public void testFalseAndUndefLiteralKeywords() {
        myFixture.configureByText("main.scad", "x = <caret>");
        assertContainsKeyword("false");
        assertContainsKeyword("undef");
    }

    public void testKeywordsNotOfferedInsideImportPath() {
        myFixture.configureByText("main.scad", "include <inc<caret>");
        assertDoesNotContainKeyword("include");
        assertDoesNotContainKeyword("true");
    }

    public void testKeywordsNotOfferedForModuleParameterName() {
        myFixture.configureByText("main.scad", """
                module foo(bar) { cube(1); }
                module other(<caret>) { cube(1); }
                """);
        assertDoesNotContainKeyword("module");
        assertDoesNotContainKeyword("true");
    }

    public void testControlFlowKeywordsAtFileStart() {
        myFixture.configureByText("main.scad", "<caret>");
        assertContainsKeyword("if");
        assertContainsKeyword("for");
        assertContainsKeyword("else");
        assertContainsKeyword("let");
    }

    public void testControlFlowKeywordsWithPrefix() {
        myFixture.configureByText("main.scad", "fo<caret>");
        assertContainsKeyword("for");
    }

    public void testControlFlowKeywordsNotOfferedInExpression() {
        myFixture.configureByText("main.scad", "x = <caret>");
        assertDoesNotContainKeyword("if");
        assertDoesNotContainKeyword("for");
    }

    private void assertContainsKeyword(String keyword) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull(items);
        for (final LookupElement item : items) {
            if (keyword.equals(item.getLookupString())) {
                return;
            }
        }
        fail("Expected completion to include keyword " + keyword + " but got: " + myFixture.getLookupElementStrings());
    }

    private void assertDoesNotContainKeyword(String keyword) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull(items);
        for (final LookupElement item : items) {
            if (keyword.equals(item.getLookupString())) {
                fail("Expected completion to exclude keyword " + keyword + " but got: " + myFixture.getLookupElementStrings());
            }
        }
    }
}
