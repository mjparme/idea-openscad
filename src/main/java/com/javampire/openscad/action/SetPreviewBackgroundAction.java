package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.javampire.openscad.editor.OpenSCADPreviewFileEditor;
import com.javampire.openscad.editor.PreviewBackground;
import org.jetbrains.annotations.NotNull;

/**
 * Sets the preview scene background from the toolbar background dropdown.
 */
public class SetPreviewBackgroundAction extends OpenSCADAction {

    private final PreviewBackground background;

    public SetPreviewBackgroundAction(@NotNull final PreviewBackground background) {
        this.background = background;
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        final Presentation presentation = checkOpenSCADPrerequisites(event);
        if (!presentation.isVisible()) {
            return;
        }
        final OpenSCADPreviewFileEditor previewEditor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
        final boolean selected = previewEditor != null
                && background == previewEditor.getEditorConfig().getPreviewBackground();
        presentation.setText((selected ? "✓ " : "") + background.getDisplayName());
        presentation.setDescription("Use the " + background.getDisplayName() + " preview background");
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final OpenSCADPreviewFileEditor previewEditor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
        if (previewEditor != null) {
            previewEditor.getEditorConfig().setPreviewBackground(background);
        }
    }
}
