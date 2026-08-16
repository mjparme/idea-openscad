package com.javampire.openscad.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OpenSCADImportPathCompletionTest extends BasePlatformTestCase {

    public void testSameDirectoryIncludePathCompletion() {
        myFixture.configureByText("include_lib.scad", "lib_var = 1;");
        myFixture.configureByText("include_main.scad", "include <include_lib.scad>");
        moveCaretToEndOfImportPathPrefix("include_l");
        assertUtilSuggestsPath("include_l", "include_lib.scad");
        assertContainsPath("include_lib.scad");
    }

    public void testSubdirectoryUsePathCompletion() {
        myFixture.addFileToProject("lib/cubes.scad", "module roundedCube() { cube(1); }");
        myFixture.configureByText("main.scad", "use <lib/cubes.scad>");
        moveCaretToEndOfImportPathPrefix("lib/cu");
        assertContainsPath("lib/cubes.scad");
    }

    public void testParentRelativeUsePathCompletion() {
        myFixture.addFileToProject("lib/cubes.scad", "module roundedCube() { cube(1); }");
        myFixture.addFileToProject("fiber-arts/thread-holder/main.scad", "use <../../lib/cubes.scad>");
        myFixture.configureFromTempProjectFile("fiber-arts/thread-holder/main.scad");
        moveCaretToEndOfImportPathPrefix("../../lib/cu");
        assertUtilSuggestsPath("../../lib/cu", "../../lib/cubes.scad");
        assertContainsPath("../../lib/cubes.scad");
    }

    public void testSubdirectoryCompletionOffersDirectoryEntry() {
        myFixture.addFileToProject("lib/cubes.scad", "module roundedCube() { cube(1); }");
        myFixture.configureByText("main.scad", "use <lib/cubes.scad>");
        moveCaretToEndOfImportPathPrefix("l");
        assertUtilSuggestsPath("l", "lib/");
        assertContainsPath("lib/");
    }

    public void testImportPathCompletionDoesNotOfferModuleNames() {
        myFixture.configureByText("use_lib.scad", """
                module libModule() { cube(1); }
                """);
        myFixture.configureByText("use_main.scad", "use <use_lib.scad>");
        moveCaretToEndOfImportPathPrefix("use_l");
        assertContainsPath("use_lib.scad");
        assertDoesNotContainPath("libModule");
    }

    private void moveCaretToEndOfImportPathPrefix(String prefix) {
        final String text = myFixture.getFile().getText();
        final int start = text.indexOf('<' + prefix.substring(0, 1));
        assertTrue("Expected import path in file text: " + text, start >= 0);
        final int prefixStart = text.indexOf(prefix, start + 1);
        assertTrue("Expected prefix '" + prefix + "' in file text: " + text, prefixStart >= 0);
        myFixture.getEditor().getCaretModel().moveToOffset(prefixStart + prefix.length());
    }

    private void assertUtilSuggestsPath(String partialPath, String expectedPath) {
        final var suggestions = OpenSCADImportPathCompletionUtil.suggestPaths(myFixture.getFile(), partialPath);
        final var paths = suggestions.stream().map(OpenSCADImportPathCompletionUtil.ImportPathSuggestion::path).toList();
        assertTrue("Expected util suggestions to include " + expectedPath + " but got: " + paths, paths.contains(expectedPath));
    }

    private void assertContainsPath(String path) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull("Completion returned null at offset " + myFixture.getCaretOffset(), items);
        for (final LookupElement item : items) {
            if (path.equals(item.getLookupString())) {
                return;
            }
        }
        fail("Expected completion to include " + path + " but got: " + myFixture.getLookupElementStrings());
    }

    private void assertDoesNotContainPath(String path) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull(items);
        for (final LookupElement item : items) {
            if (path.equals(item.getLookupString())) {
                fail("Expected completion to exclude " + path + " but got: " + myFixture.getLookupElementStrings());
            }
        }
    }
}
