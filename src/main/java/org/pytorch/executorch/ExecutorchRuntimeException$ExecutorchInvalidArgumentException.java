/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import org.pytorch.executorch.ExecutorchRuntimeException;

public static class ExecutorchRuntimeException.ExecutorchInvalidArgumentException
extends IllegalArgumentException {
    private final int errorCode = 18;

    public ExecutorchRuntimeException.ExecutorchInvalidArgumentException(String details) {
        super(ExecutorchRuntimeException.ErrorHelper.formatMessage(18, details));
    }

    public int getErrorCode() {
        return 18;
    }
}
