package com.javampire.openscad.action;

import junit.framework.TestCase;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

public class OpenSCADExecutorTest extends TestCase {

    public void testStartDetachedDoesNotWaitForProcessExit() throws Exception {
        final File script = File.createTempFile("openscad-detached-", ".sh");
        script.deleteOnExit();
        Files.writeString(script.toPath(), "#!/bin/sh\nsleep 3\n");
        assertTrue(script.setExecutable(true));

        final OpenSCADExecutor executor = new OpenSCADExecutor(script.getAbsolutePath(), Collections.emptyList());
        final long startNanos = System.nanoTime();
        OpenSCADExecutor.startDetached(executor);
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertNull(executor.getException());
        assertTrue("Detached launch should return before the child process exits", elapsedMs < 1500);
    }

    public void testOpenActionDoesNotWaitForProcessCompletion() {
        assertFalse(new OpenAction().waitForProcessCompletion());
    }
}
