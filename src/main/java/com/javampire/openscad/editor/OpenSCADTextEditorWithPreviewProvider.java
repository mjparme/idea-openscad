package com.javampire.openscad.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.javampire.openscad.settings.OpenSCADSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

public class OpenSCADTextEditorWithPreviewProvider implements AsyncFileEditorProvider, DumbAware {

    private static final Logger LOG = Logger.getInstance(OpenSCADTextEditorWithPreviewProvider.class);

    @NotNull
    private final TextEditorProvider textEditorProvider;
    @NotNull
    private final OpenSCADPreviewFileEditorProvider previewEditorProvider;

    public OpenSCADTextEditorWithPreviewProvider() {
        textEditorProvider = new PsiAwareTextEditorProvider();
        previewEditorProvider = new OpenSCADPreviewFileEditorProvider();
    }

    @Override
    public boolean accept(@NotNull final Project project, @NotNull final VirtualFile file) {
        if (!textEditorProvider.accept(project, file)) {
            return false;
        }
        if (!previewEditorProvider.accept(project, file)) {
            final OpenSCADSettings settings = OpenSCADSettings.getInstance();
            LOG.warn("OpenSCAD split preview not used: allowPreview=" + settings.isAllowPreviewEditor()
                    + ", jcef=" + JcefSupport.isSupported());
            return false;
        }
        if (FileIndexFacade.getInstance(project).getModuleForFile(file) == null) {
            LOG.warn("OpenSCAD split preview not used: file is outside project modules (" + file.getPath() + ")");
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull final Project project, @NotNull final VirtualFile file) {
        return createEditorAsync(project, file).build();
    }

    @Override
    public @NotNull Builder createEditorAsync(@NotNull final Project project, @NotNull final VirtualFile file) {
        return new Builder() {
            @Override
            public FileEditor build() {
                final TextEditor textEditor = (TextEditor) textEditorProvider.createEditor(project, file);
                final FileEditor rendererEditor = previewEditorProvider.createEditor(project, file);
                return new TextEditorWithPreview(
                        textEditor,
                        rendererEditor,
                        "OpenSCADTextEditorWithPreview",
                        TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW,
                        false
                );
            }
        };
    }

    @Override
    @NonNls
    public @NotNull String getEditorTypeId() {
        return OpenSCADTextEditorWithPreviewProvider.class.getSimpleName();
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
