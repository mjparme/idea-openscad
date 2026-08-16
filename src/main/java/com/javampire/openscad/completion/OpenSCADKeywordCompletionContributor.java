package com.javampire.openscad.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.javampire.openscad.psi.OpenSCADArgDeclaration;
import com.javampire.openscad.psi.OpenSCADExpr;
import com.javampire.openscad.psi.OpenSCADBuiltinExpr;
import com.javampire.openscad.psi.OpenSCADBuiltinObj;
import com.javampire.openscad.psi.OpenSCADFile;
import com.javampire.openscad.psi.OpenSCADFunctionCallExpr;
import com.javampire.openscad.psi.OpenSCADFunctionDeclaration;
import com.javampire.openscad.psi.OpenSCADFunctionNameRef;
import com.javampire.openscad.psi.OpenSCADLiteralExpr;
import com.javampire.openscad.psi.OpenSCADModuleDeclaration;
import com.javampire.openscad.psi.OpenSCADModuleOpNameRef;
import com.javampire.openscad.psi.OpenSCADParenExpr;
import com.javampire.openscad.psi.OpenSCADTypes;
import com.javampire.openscad.psi.OpenSCADVariableRefExpr;
import org.jetbrains.annotations.NotNull;

/**
 * Completes OpenSCAD language keywords: imports, declarations, control flow, and literal constants.
 */
final class OpenSCADKeywordCompletionContributor {

    private static final String[] IMPORT_KEYWORDS = {"include", "use"};
    private static final String[] DECLARATION_KEYWORDS = {"function", "module"};
    private static final String[] CONTROL_FLOW_KEYWORDS = {
        "assign", "each", "else", "for", "if", "intersection_for", "let"
    };
    private static final String[] LITERAL_KEYWORDS = {"false", "true", "undef"};

    private static final InsertHandler<LookupElement> IMPORT_KEYWORD_INSERT_HANDLER = (context, item) -> {
        final InsertionContext insertionContext = context;
        final int offset = insertionContext.getTailOffset();
        final var document = insertionContext.getEditor().getDocument();
        if (offset >= document.getTextLength() || document.getText().charAt(offset) != '<') {
            document.insertString(offset, " <");
            insertionContext.getEditor().getCaretModel().moveToOffset(offset + 2);
        }
    };

    private static final InsertHandler<LookupElement> DECLARATION_KEYWORD_INSERT_HANDLER = (context, item) -> {
        final int offset = context.getTailOffset();
        final var document = context.getEditor().getDocument();
        if (offset >= document.getTextLength() || !Character.isWhitespace(document.getText().charAt(offset))) {
            document.insertString(offset, " ");
            context.getEditor().getCaretModel().moveToOffset(offset + 1);
        }
    };

    // if (), for (), let (), assign (), intersection_for ()
    private static final InsertHandler<LookupElement> CONTROL_FLOW_PAREN_INSERT_HANDLER = (context, item) -> {
        final int offset = context.getTailOffset();
        final var document = context.getEditor().getDocument();
        if (offset < document.getTextLength() && document.getText().charAt(offset) == '(') {
            return;
        }
        document.insertString(offset, " (");
        context.getEditor().getCaretModel().moveToOffset(offset + 2);
    };

    private OpenSCADKeywordCompletionContributor() {
    }

    static void addKeywordCompletions(@NotNull final CompletionParameters parameters,
                                    @NotNull final CompletionResultSet result) {
        final PsiElement position = parameters.getPosition();
        final PsiFile file = parameters.getOriginalFile();

        if (allowsLiteralKeyword(position, file, parameters.getOffset())) {
            for (final String keyword : LITERAL_KEYWORDS) {
                result.addElement(toKeywordLookup(keyword, "literal"));
            }
        }

        if (allowsStatementKeyword(position, file, parameters.getOffset())) {
            for (final String keyword : IMPORT_KEYWORDS) {
                result.addElement(toKeywordLookup(keyword, "import")
                    .withInsertHandler(IMPORT_KEYWORD_INSERT_HANDLER));
            }
            for (final String keyword : DECLARATION_KEYWORDS) {
                result.addElement(toKeywordLookup(keyword, "declaration")
                    .withInsertHandler(DECLARATION_KEYWORD_INSERT_HANDLER));
            }
        }

        if (allowsControlFlowKeyword(position, file, parameters.getOffset())) {
            for (final String keyword : CONTROL_FLOW_KEYWORDS) {
                final LookupElementBuilder builder = toKeywordLookup(keyword, "control flow");
                if ("else".equals(keyword) || "each".equals(keyword)) {
                    result.addElement(builder.withInsertHandler(DECLARATION_KEYWORD_INSERT_HANDLER));
                } else {
                    result.addElement(builder.withInsertHandler(CONTROL_FLOW_PAREN_INSERT_HANDLER));
                }
            }
        }
    }

    private static @NotNull LookupElementBuilder toKeywordLookup(@NotNull final String keyword,
                                                                 @NotNull final String kind) {
        return LookupElementBuilder.create(keyword)
            .withBoldness(true)
            .withTypeText(kind, true);
    }

    private static boolean allowsLiteralKeyword(@NotNull final PsiElement position,
                                                @NotNull final PsiFile file,
                                                final int offset) {
        if (isInsideImportPath(file, offset)) {
            return false;
        }
        if (PsiTreeUtil.getParentOfType(position, OpenSCADModuleDeclaration.class, OpenSCADFunctionDeclaration.class) != null
            && isDeclarationNamePosition(position)) {
            return false;
        }
        return PsiTreeUtil.getParentOfType(position, OpenSCADArgDeclaration.class) == null;
    }

    private static boolean allowsStatementKeyword(@NotNull final PsiElement position,
                                                  @NotNull final PsiFile file,
                                                  final int offset) {
        if (isInsideImportPath(file, offset)) {
            return false;
        }
        if (PsiTreeUtil.getParentOfType(position, OpenSCADArgDeclaration.class) != null) {
            return false;
        }
        if (PsiTreeUtil.getParentOfType(position, OpenSCADModuleOpNameRef.class, OpenSCADFunctionNameRef.class) != null) {
            return false;
        }
        if (PsiTreeUtil.getParentOfType(position, OpenSCADLiteralExpr.class) != null) {
            return false;
        }

        PsiElement context = position;
        while (context != null) {
            if (context instanceof OpenSCADFile
                || context.getNode() != null && context.getNode().getElementType() == OpenSCADTypes.BLOCK_OBJ) {
                return true;
            }
            if (context instanceof OpenSCADModuleDeclaration || context instanceof OpenSCADFunctionDeclaration) {
                return !isDeclarationNamePosition(position);
            }
            if (context instanceof OpenSCADParenExpr
                || context instanceof OpenSCADFunctionCallExpr
                || context instanceof OpenSCADVariableRefExpr
                || context instanceof OpenSCADBuiltinExpr
                || context instanceof OpenSCADBuiltinObj) {
                return false;
            }
            context = context.getParent();
        }
        return file instanceof OpenSCADFile;
    }

    private static boolean allowsControlFlowKeyword(@NotNull final PsiElement position,
                                                    @NotNull final PsiFile file,
                                                    final int offset) {
        if (!allowsStatementKeyword(position, file, offset)) {
            return false;
        }
        return PsiTreeUtil.getParentOfType(position, OpenSCADExpr.class) == null;
    }

    private static boolean isInsideImportPath(@NotNull final PsiFile file, final int offset) {
        return OpenSCADImportPathCompletionUtil.findImportPathRefAtOffset(file, offset) != null
            || OpenSCADImportPathCompletionUtil.isInsideIncompleteImportPath(file, offset);
    }

    private static boolean isDeclarationNamePosition(@NotNull final PsiElement position) {
        final PsiElement parent = position.getParent();
        if (parent instanceof OpenSCADModuleDeclaration module) {
            final var nameIdentifier = module.getNameIdentifier();
            return nameIdentifier != null && nameIdentifier.getTextRange().contains(position.getTextRange());
        }
        if (parent instanceof OpenSCADFunctionDeclaration function) {
            final var nameIdentifier = function.getNameIdentifier();
            return nameIdentifier != null && nameIdentifier.getTextRange().contains(position.getTextRange());
        }
        return false;
    }
}
