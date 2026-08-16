package com.javampire.openscad.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.javampire.openscad.OpenSCADFileType;
import com.javampire.openscad.psi.OpenSCADImportPathRefElement;
import org.jetbrains.annotations.NotNull;

/**
 * Completes file paths inside {@code use <...>} and {@code include <...>} statements.
 */
final class OpenSCADImportPathCompletionContributor {

    private static final InsertHandler<LookupElement> DIRECTORY_INSERT_HANDLER = (context, item) -> {
        final int tailOffset = context.getTailOffset();
        final var document = context.getEditor().getDocument();
        if (tailOffset > 0 && tailOffset <= document.getTextLength() && document.getText().charAt(tailOffset - 1) != '/') {
            document.insertString(tailOffset, "/");
            context.getEditor().getCaretModel().moveToOffset(tailOffset + 1);
        }
    };

    private OpenSCADImportPathCompletionContributor() {
    }

    static void addImportPathCompletions(@NotNull final CompletionParameters parameters,
                                         @NotNull final CompletionResultSet result) {
        final var file = parameters.getOriginalFile();
        final int offset = parameters.getOffset();
        final OpenSCADImportPathRefElement pathRef = OpenSCADImportPathCompletionUtil.findImportPathRefAtOffset(file, offset);
        final String partialPath;
        if (pathRef != null) {
            partialPath = OpenSCADImportPathCompletionUtil.partialImportPathAtOffset(
                pathRef,
                offset,
                result.getPrefixMatcher().getPrefix()
            );
        }
        else {
            final String extracted = OpenSCADImportPathCompletionUtil.extractPartialImportPathFromText(file, offset);
            if (extracted == null) {
                return;
            }
            partialPath = extracted;
        }

        final CompletionResultSet pathResult = partialPath.isEmpty()
            ? result
            : result.withPrefixMatcher(partialPath);
        for (final OpenSCADImportPathCompletionUtil.ImportPathSuggestion suggestion :
            OpenSCADImportPathCompletionUtil.suggestPaths(file, partialPath)) {
            LookupElementBuilder builder = LookupElementBuilder.create(suggestion.path())
                .withIcon(OpenSCADFileType.INSTANCE.getIcon());
            if (suggestion.directory()) {
                builder = builder.withInsertHandler(DIRECTORY_INSERT_HANDLER);
            }
            pathResult.addElement(builder);
        }
    }
}
