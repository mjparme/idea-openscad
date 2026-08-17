package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.javampire.openscad.OpenSCADFileType;
import com.javampire.openscad.OpenSCADLanguage;
import com.javampire.openscad.editor.OpenSCADPreviewFileEditor;
import com.javampire.openscad.settings.OpenSCADSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class OpenSCADAction extends AnAction implements DumbAware {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Set the presentation enable and visible if OpenSCAD executable is found and if the target file is an OpenSCAD one.
     *
     * @param event Action event.
     * @return Event presentation.
     */
    protected Presentation checkOpenSCADPrerequisites(@NotNull final AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        if (!OpenSCADSettings.getInstance().hasExecutable()) {
            presentation.setEnabledAndVisible(false);
            return presentation;
        }
        if (isSupportedActionPlace(event)) {
            presentation.setEnabledAndVisible(isOpenScadContext(event));
        } else {
            presentation.setEnabledAndVisible(false);
        }
        return presentation;
    }

    static boolean isSupportedActionPlace(@NotNull final AnActionEvent event) {
        final ActionUiKind uiKind = event.getUiKind();
        return uiKind instanceof ActionUiKind.Toolbar || uiKind instanceof ActionUiKind.Popup;
    }

    static boolean isOpenScadContext(@NotNull final AnActionEvent event) {
        if (event.getData(OpenSCADDataKeys.PREVIEW_EDITOR) != null) {
            return true;
        }
        final PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null && psiFile.getLanguage().is(OpenSCADLanguage.INSTANCE)) {
            return true;
        }
        final VirtualFile virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        return isOpenScadVirtualFile(virtualFile);
    }

    static boolean isOpenScadVirtualFile(@Nullable final VirtualFile virtualFile) {
        if (virtualFile == null) {
            return false;
        }
        if (OpenSCADFileType.INSTANCE.equals(virtualFile.getFileType())) {
            return true;
        }
        return "scad".equalsIgnoreCase(virtualFile.getExtension());
    }


    /**
     * Returns the preview file editor for the given file if there is one opened.
     *
     * @param project  Project.
     * @param scadFile Scad file.
     * @return Preview file editor.
     */
    protected static OpenSCADPreviewFileEditor getOpenSCADPreviewFileEditor(final @NotNull Project project, final @NotNull VirtualFile scadFile) {
        final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(scadFile);
        if (selectedEditor instanceof TextEditorWithPreview) {
            final FileEditor previewEditor = ((TextEditorWithPreview) selectedEditor).getPreviewEditor();
            if (previewEditor instanceof OpenSCADPreviewFileEditor) {
                return ((OpenSCADPreviewFileEditor) previewEditor);
            }
        }
        return null;
    }
}
