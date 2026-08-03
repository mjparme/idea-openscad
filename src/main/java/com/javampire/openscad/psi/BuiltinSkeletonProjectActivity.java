package com.javampire.openscad.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VirtualFileManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers a VFS listener once so skeleton edits invalidate completion caches without restarting the IDE.
 */
public final class BuiltinSkeletonProjectActivity implements ProjectActivity {

    private static final AtomicBoolean LISTENER_REGISTERED = new AtomicBoolean();

    @Override
    public @Nullable Object execute(@NotNull final Project project, @NotNull final Continuation<? super Unit> $completion) {
        if (LISTENER_REGISTERED.compareAndSet(false, true)) {
            ApplicationManager.getApplication().getMessageBus()
                    .connect()
                    .subscribe(VirtualFileManager.VFS_CHANGES, new BuiltinSkeletonVfsListener());
        }
        return Unit.INSTANCE;
    }
}
