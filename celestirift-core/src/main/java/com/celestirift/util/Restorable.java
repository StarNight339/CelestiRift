package com.celestirift.util;

/**
 * Content holder that can be rolled back. {@link #snapshot()} copies the current
 * state and returns an action that puts it back - used by safe reload to keep the
 * old content when the new files are broken.
 */
public interface Restorable {
    Runnable snapshot();
}
