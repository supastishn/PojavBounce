/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import org.pytorch.executorch.Module;

static class ExecutorchRuntimeException.ErrorHelper {
    private static final boolean ENABLE_READ_LOG_BUFFER_LOGS = true;
    private static final StringBuilder sb = new StringBuilder();

    ExecutorchRuntimeException.ErrorHelper() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static String formatMessage(int errorCode, String details) {
        StringBuilder stringBuilder = sb;
        synchronized (stringBuilder) {
            sb.setLength(0);
            String baseMessage = (String)ERROR_CODE_MESSAGES.get(errorCode);
            if (baseMessage == null) {
                baseMessage = "Unknown error code 0x" + Integer.toHexString(errorCode);
            }
            sb.append("[Executorch Error 0x").append(Integer.toHexString(errorCode)).append("] ").append(baseMessage).append(": ").append(details);
            try {
                String[] logEntries = Module.readLogBufferStatic();
                if (logEntries != null && logEntries.length > 0) {
                    sb.append("\n Detailed logs:\n");
                }
                ExecutorchRuntimeException.ErrorHelper.formatLogEntries(sb, logEntries);
            }
            catch (Exception e) {
                sb.append("Failed to retrieve detailed logs: ").append(e.getMessage());
            }
            return sb.toString();
        }
    }

    private static void formatLogEntries(StringBuilder sb, String[] logEntries) {
        if (logEntries == null || logEntries.length == 0) {
            sb.append("No detailed logs available.");
            return;
        }
        for (String entry : logEntries) {
            sb.append(entry).append("\n");
        }
    }
}
