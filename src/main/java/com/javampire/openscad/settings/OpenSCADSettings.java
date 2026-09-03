package com.javampire.openscad.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@State(name = "OpenSCADSettings", storages = @Storage("OpenSCADSettings.xml"))
public class OpenSCADSettings implements PersistentStateComponent<OpenSCADSettings> {

    private String openSCADExecutable = null;
    private boolean allowPreviewEditor = false;
    private boolean fillNamedArgumentsOnModuleCompletion = false;
    private List<String> previewFontDirectories = new ArrayList<>();

    public static OpenSCADSettings getInstance() {
        return ApplicationManager.getApplication().getService(OpenSCADSettings.class);
    }

    @Override
    public OpenSCADSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull final OpenSCADSettings state) {
        XmlSerializerUtil.copyBean(state, this);
        openSCADExecutable = normalizeExecutablePath(openSCADExecutable);
    }

    @Nullable
    public String getOpenSCADExecutable() {
        return openSCADExecutable;
    }

    public void setOpenSCADExecutable(@Nullable final String openSCADExecutable) {
        this.openSCADExecutable = normalizeExecutablePath(openSCADExecutable);
    }

    public boolean isAllowPreviewEditor() {
        return allowPreviewEditor;
    }

    public void setAllowPreviewEditor(boolean allowPreviewEditor) {
        this.allowPreviewEditor = allowPreviewEditor;
    }

    public boolean isFillNamedArgumentsOnModuleCompletion() {
        return fillNamedArgumentsOnModuleCompletion;
    }

    public void setFillNamedArgumentsOnModuleCompletion(final boolean fillNamedArgumentsOnModuleCompletion) {
        this.fillNamedArgumentsOnModuleCompletion = fillNamedArgumentsOnModuleCompletion;
    }

    @NotNull
    public List<String> getPreviewFontDirectories() {
        return previewFontDirectories != null ? previewFontDirectories : Collections.emptyList();
    }

    public void setPreviewFontDirectories(@NotNull final List<String> previewFontDirectories) {
        this.previewFontDirectories = new ArrayList<>(previewFontDirectories);
    }

    public boolean hasExecutable() {
        final String path = normalizeExecutablePath(getOpenSCADExecutable());
        if (StringUtil.isEmptyOrSpaces(path)) {
            return false;
        }
        final File file = new File(path);
        return file.isFile() && file.canExecute();
    }

    @Nullable
    static String normalizeExecutablePath(@Nullable final String path) {
        if (StringUtil.isEmptyOrSpaces(path)) {
            return null;
        }
        final String trimmed = path.trim();
        final File file = new File(trimmed);
        if (file.isFile()) {
            return file.getPath();
        }
        if (trimmed.endsWith(".app")) {
            for (final String executableName : new String[]{"OpenSCAD", "openscad"}) {
                final File macBinary = new File(file, "Contents/MacOS/" + executableName);
                if (macBinary.isFile()) {
                    return macBinary.getPath();
                }
            }
        }
        return trimmed;
    }
}