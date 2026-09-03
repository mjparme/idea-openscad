package com.javampire.openscad.settings;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OpenSCADSettingsConfigurable implements SearchableConfigurable.Parent, Configurable.NoScroll {

    private static final int PREVIEW_FONT_LIST_VISIBLE_ROWS = 6;

    private final Project myProject;
    private JPanel settingsPanel;
    private TextFieldWithBrowseButton openSCADExecutablePath;
    private JCheckBox allowPreviewEditor;
    private JLabel allowPreviewEditorText;
    private JCheckBox fillNamedArgumentsOnModuleCompletion;
    private JLabel fillNamedArgumentsOnModuleCompletionText;
    private JPanel previewFontDirectoriesPanel;
    private DefaultListModel<String> previewFontDirectoriesModel;
    private JList<String> previewFontDirectoriesList;

    OpenSCADSettingsConfigurable(final Project project) {
        myProject = project;

        openSCADExecutablePath.getTextField().addActionListener(e -> {
            if (openSCADExecutablePath.getText().isEmpty()) {
                allowPreviewEditor.setEnabled(false);
                allowPreviewEditor.setSelected(false);
                allowPreviewEditorText.setEnabled(false);
            } else {
                allowPreviewEditor.setEnabled(true);
                allowPreviewEditorText.setEnabled(true);
            }
        });
    }

    @NotNull
    @Override
    public String getId() {
        return getClass().getName();
    }

    @Nullable
    @Override
    public Runnable enableSearch(final String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "OpenSCAD";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return settingsPanel;
    }

    @Override
    public boolean isModified() {
        final OpenSCADSettings openSCADSettings = OpenSCADSettings.getInstance();
        return allowPreviewEditor.isSelected() != openSCADSettings.isAllowPreviewEditor()
                || fillNamedArgumentsOnModuleCompletion.isSelected() != openSCADSettings.isFillNamedArgumentsOnModuleCompletion()
                || !openSCADExecutablePath.getText().equals(openSCADSettings.getOpenSCADExecutable())
                || !getPreviewFontDirectoriesFromUi().equals(openSCADSettings.getPreviewFontDirectories());
    }

    @Override
    public void apply() {
        final OpenSCADSettings openSCADSettings = OpenSCADSettings.getInstance();
        openSCADSettings.setOpenSCADExecutable(openSCADExecutablePath.getText());
        openSCADSettings.setAllowPreviewEditor(allowPreviewEditor.isSelected());
        openSCADSettings.setFillNamedArgumentsOnModuleCompletion(fillNamedArgumentsOnModuleCompletion.isSelected());
        openSCADSettings.setPreviewFontDirectories(getPreviewFontDirectoriesFromUi());
        OpenSCADInfo.reset();
        if (openSCADSettings.hasExecutable()) {
            OpenSCADSettingsStartupActivity.updateOpenSCADLibraries(myProject);
        }
    }

    @Override
    public void reset() {
        final OpenSCADSettings openSCADSettings = OpenSCADSettings.getInstance();
        final String openSCADExecutable = openSCADSettings.getOpenSCADExecutable();
        openSCADExecutablePath.setText(openSCADExecutable != null ? openSCADExecutable : "");
        allowPreviewEditor.setSelected(openSCADSettings.isAllowPreviewEditor());
        fillNamedArgumentsOnModuleCompletion.setSelected(openSCADSettings.isFillNamedArgumentsOnModuleCompletion());
        previewFontDirectoriesModel.clear();
        for (final String directory : openSCADSettings.getPreviewFontDirectories()) {
            previewFontDirectoriesModel.addElement(directory);
        }
    }

    @NotNull
    private List<String> getPreviewFontDirectoriesFromUi() {
        final List<String> directories = new ArrayList<>(previewFontDirectoriesModel.getSize());
        for (int i = 0; i < previewFontDirectoriesModel.getSize(); i++) {
            directories.add(previewFontDirectoriesModel.getElementAt(i));
        }
        return directories;
    }

    private void createUIComponents() {
        openSCADExecutablePath = new TextFieldWithBrowseButton();

        final FileChooserDescriptor executableDescriptor = FileChooserDescriptorFactory.singleFile()
                .withFileFilter(virtualFile -> virtualFile.isInLocalFileSystem() && new File(virtualFile.getPath()).canExecute())
                .withTitle("Choose OpenSCAD Executable")
                .withDescription("Choose OpenSCAD executable");
        openSCADExecutablePath.addBrowseFolderListener(myProject, executableDescriptor);

        previewFontDirectoriesModel = new DefaultListModel<>();
        previewFontDirectoriesList = new JList<>(previewFontDirectoriesModel);
        previewFontDirectoriesList.setVisibleRowCount(PREVIEW_FONT_LIST_VISIBLE_ROWS);

        final JButton addFontDirectoryButton = new JButton("Add...");
        addFontDirectoryButton.addActionListener(e -> {
            final FileChooserDescriptor directoryDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle("Choose preview font directory")
                    .withDescription("Directory containing .ttf, .otf, or .ttc files for WASM preview");
            final VirtualFile chosen = FileChooser.chooseFile(directoryDescriptor, myProject, null);
            if (chosen != null) {
                final String path = chosen.getPath();
                if (!previewFontDirectoriesModel.contains(path)) {
                    previewFontDirectoriesModel.addElement(path);
                }
            }
        });

        final JButton removeFontDirectoryButton = new JButton("Remove");
        removeFontDirectoryButton.addActionListener(e -> {
            final int selectedIndex = previewFontDirectoriesList.getSelectedIndex();
            if (selectedIndex >= 0) {
                previewFontDirectoriesModel.remove(selectedIndex);
            }
        });

        final JPanel fontDirectoryButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fontDirectoryButtons.add(addFontDirectoryButton);
        fontDirectoryButtons.add(removeFontDirectoryButton);

        final JBScrollPane fontDirectoriesScrollPane = new JBScrollPane(previewFontDirectoriesList);
        final int rowHeight = previewFontDirectoriesList.getFixedCellHeight() > 0
                ? previewFontDirectoriesList.getFixedCellHeight()
                : previewFontDirectoriesList.getFontMetrics(previewFontDirectoriesList.getFont()).getHeight() + JBUI.scale(4);
        final int listHeight = rowHeight * PREVIEW_FONT_LIST_VISIBLE_ROWS;
        final Dimension listViewportSize = new Dimension(-1, listHeight);
        fontDirectoriesScrollPane.setMinimumSize(listViewportSize);
        fontDirectoriesScrollPane.setPreferredSize(listViewportSize);

        previewFontDirectoriesPanel = new JPanel(new BorderLayout(0, 4));
        previewFontDirectoriesPanel.setBorder(JBUI.Borders.emptyTop(6));
        previewFontDirectoriesPanel.add(fontDirectoriesScrollPane, BorderLayout.CENTER);
        previewFontDirectoriesPanel.add(fontDirectoryButtons, BorderLayout.SOUTH);
    }

    @NotNull
    @Override
    public Configurable @NotNull [] getConfigurables() {
        return new Configurable[0];
    }

    @Override
    public boolean hasOwnContent() {
        return true;
    }
}
