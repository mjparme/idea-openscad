package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.javampire.openscad.editor.OpenSCADPreviewFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders preview geometry in-browser via WebAssembly (no native OpenSCAD executable required).
 */
public class GeneratePreviewAction extends OpenSCADAction implements DumbAware {

    public final static String TEXT = "Generate Preview";

    protected OpenSCADPreviewFileEditor previewFileEditor;

    @Override
    public void update(@NotNull final AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        previewFileEditor = resolvePreviewEditor(event);
        final boolean visible = previewFileEditor != null;
        presentation.setEnabledAndVisible(visible);
        if (visible) {
            presentation.setText(TEXT);
            presentation.setDescription("Render preview in the browser using WebAssembly");
        }
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        if (previewFileEditor == null) {
            previewFileEditor = resolvePreviewEditor(event);
        }
        if (previewFileEditor != null) {
            previewFileEditor.renderWasmPreview();
        }
    }

    @Nullable
    protected OpenSCADPreviewFileEditor resolvePreviewEditor(@NotNull final AnActionEvent event) {
        OpenSCADPreviewFileEditor editor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
        if (editor != null) {
            return editor;
        }
        final Project project = event.getProject();
        final VirtualFile scadFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project != null && scadFile != null) {
            return getOpenSCADPreviewFileEditor(project, scadFile);
        }
        return null;
    }
}
