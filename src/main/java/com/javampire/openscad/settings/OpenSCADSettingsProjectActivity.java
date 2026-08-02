package com.javampire.openscad.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenSCADSettingsProjectActivity implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> $completion) {
        OpenSCADSettingsStartupActivity.runStartup(project);
        return Unit.INSTANCE;
    }
}
