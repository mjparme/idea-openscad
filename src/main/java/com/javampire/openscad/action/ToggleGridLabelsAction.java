package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.javampire.openscad.OpenSCADIcons;
import com.javampire.openscad.editor.OpenSCADPreviewFileEditor;
import org.jetbrains.annotations.NotNull;

/**
 * Can be called from preview toolbar. Toggles on and off the preview grid tick labels.
 */
public class ToggleGridLabelsAction extends OpenSCADAction {

    @Override
    public void update(@NotNull final AnActionEvent event) {
        final Presentation presentation = checkOpenSCADPrerequisites(event);
        if (presentation.isVisible()) {
            presentation.setText("Toggle Grid Labels");
            final OpenSCADPreviewFileEditor previewEditor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
            final boolean gridShown = previewEditor != null
                    && Boolean.TRUE.equals(previewEditor.getEditorConfig().getShowGrid());
            final boolean labelsShown = previewEditor != null
                    && Boolean.TRUE.equals(previewEditor.getEditorConfig().getShowGridLabels());
            presentation.setEnabled(gridShown);
            presentation.setDescription(gridShown
                    ? "Show or hide coordinate labels on the preview grid"
                    : "Show the grid to toggle coordinate labels");
            if (labelsShown) {
                presentation.setIcon(OpenSCADIcons.TOGGLE_GRID_LABELS);
            } else {
                presentation.setIcon(OpenSCADIcons.TOGGLE_GRID_LABELS_GRAYED);
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final OpenSCADPreviewFileEditor previewEditor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
        if (previewEditor != null && Boolean.TRUE.equals(previewEditor.getEditorConfig().getShowGrid())) {
            previewEditor.getEditorConfig().toggleShowGridLabels();
        }
    }
}
