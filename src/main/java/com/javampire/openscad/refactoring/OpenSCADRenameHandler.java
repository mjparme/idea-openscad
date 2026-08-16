package com.javampire.openscad.refactoring;

import com.intellij.lang.LanguageRefactoringSupport;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.rename.RenameHandler;
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves rename targets from the caret (e.g. the {@code IDENTIFIER} token in {@code module foo()})
 * before delegating to the platform rename implementation.
 */
public class OpenSCADRenameHandler implements RenameHandler {

    @Override
    public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
        if (platformInplaceRenameHandles(dataContext)) {
            return false;
        }
        return resolveRenamableElement(dataContext) != null;
    }

    @Override
    public boolean isRenaming(@NotNull DataContext dataContext) {
        return isAvailableOnDataContext(dataContext);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, @NotNull DataContext dataContext) {
        final PsiElement element = resolveRenamableElement(dataContext);
        if (element == null || editor == null) {
            return;
        }

        final PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());
        final String defaultName = PsiElementRenameHandler.DEFAULT_NAME.getData(dataContext);
        if (defaultName != null) {
            PsiElementRenameHandler.rename(element, project, nameSuggestionContext, editor, defaultName);
        }
        else {
            PsiElementRenameHandler.invoke(element, project, nameSuggestionContext, editor);
        }
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, @NotNull DataContext dataContext) {
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (editor == null || file == null) {
            return;
        }
        invoke(project, editor, file, dataContext);
    }

    @Nullable
    private static PsiElement resolveRenamableElement(@NotNull DataContext dataContext) {
        PsiElement element = PsiElementRenameHandler.getElement(dataContext);
        if (element == null) {
            final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
            final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
            if (editor != null && file != null) {
                element = CommonRefactoringUtil.getElementAtCaret(editor, file);
            }
        }
        return OpenSCADRenameUtil.getRenamableElement(element);
    }

    /**
     * Let {@link com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler} and
     * {@link com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler} handle declaration names;
     * this handler is for references and cases the platform cannot resolve from the caret.
     */
    private static boolean platformInplaceRenameHandles(@NotNull DataContext dataContext) {
        if (VariableInplaceRenameHandler.getInitialName() != null) {
            return false;
        }
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (editor == null || file == null || !editor.getSettings().isVariableInplaceRenameEnabled()) {
            return false;
        }
        final PsiElement element = PsiElementRenameHandler.getElement(dataContext);
        if (element == null || !(element instanceof PsiNameIdentifierOwner)) {
            return false;
        }
        final RefactoringSupportProvider provider = LanguageRefactoringSupport.getInstance().forContext(element);
        if (provider == null) {
            return false;
        }
        final PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());
        return provider.isMemberInplaceRenameAvailable(element, nameSuggestionContext)
                || provider.isInplaceRenameAvailable(element, nameSuggestionContext);
    }
}
