package org.pytorch.executorch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/ExecutorchRuntimeException.class */
public class ExecutorchRuntimeException extends RuntimeException {
    public static final int OK = 0;
    public static final int INTERNAL = 1;
    public static final int INVALID_STATE = 2;
    public static final int END_OF_METHOD = 3;
    public static final int NOT_SUPPORTED = 16;
    public static final int NOT_IMPLEMENTED = 17;
    public static final int INVALID_ARGUMENT = 18;
    public static final int INVALID_TYPE = 19;
    public static final int OPERATOR_MISSING = 20;
    public static final int REGISTRATION_EXCEEDING_MAX_KERNELS = 21;
    public static final int REGISTRATION_ALREADY_REGISTERED = 22;
    public static final int NOT_FOUND = 32;
    public static final int MEMORY_ALLOCATION_FAILED = 33;
    public static final int ACCESS_FAILED = 34;
    public static final int INVALID_PROGRAM = 35;
    public static final int INVALID_EXTERNAL_DATA = 36;
    public static final int OUT_OF_RESOURCES = 37;
    public static final int DELEGATE_INVALID_COMPATIBILITY = 48;
    public static final int DELEGATE_MEMORY_ALLOCATION_FAILED = 49;
    public static final int DELEGATE_INVALID_HANDLE = 50;
    private static final Map<Integer, String> ERROR_CODE_MESSAGES;
    private final int errorCode;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "Operation successful");
        map.put(1, "Internal error");
        map.put(2, "Invalid state");
        map.put(3, "End of method reached");
        map.put(16, "Operation not supported");
        map.put(17, "Operation not implemented");
        map.put(18, "Invalid argument");
        map.put(19, "Invalid type");
        map.put(20, "Operator missing");
        map.put(21, "Exceeded max kernels");
        map.put(22, "Kernel already registered");
        map.put(32, "Resource not found");
        map.put(33, "Memory allocation failed");
        map.put(34, "Access failed");
        map.put(35, "Invalid program");
        map.put(36, "Invalid external data");
        map.put(37, "Out of resources");
        map.put(48, "Delegate invalid compatibility");
        map.put(49, "Delegate memory allocation failed");
        map.put(50, "Delegate invalid handle");
        ERROR_CODE_MESSAGES = Collections.unmodifiableMap(map);
    }

    /* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/ExecutorchRuntimeException$ErrorHelper.class */
    static class ErrorHelper {
        private static final boolean ENABLE_READ_LOG_BUFFER_LOGS = true;
        private static final StringBuilder sb = new StringBuilder();

        ErrorHelper() {
        }

        static String formatMessage(int errorCode, String details) {
            String sb2;
            synchronized (sb) {
                sb.setLength(0);
                String baseMessage = (String) ExecutorchRuntimeException.ERROR_CODE_MESSAGES.get(Integer.valueOf(errorCode));
                if (baseMessage == null) {
                    baseMessage = "Unknown error code 0x" + Integer.toHexString(errorCode);
                }
                sb.append("[Executorch Error 0x").append(Integer.toHexString(errorCode)).append("] ").append(baseMessage).append(": ").append(details);
                try {
                    String[] logEntries = Module.readLogBufferStatic();
                    if (logEntries != null && logEntries.length > 0) {
                        sb.append("\n Detailed logs:\n");
                    }
                    formatLogEntries(sb, logEntries);
                } catch (Exception e) {
                    sb.append("Failed to retrieve detailed logs: ").append(e.getMessage());
                }
                sb2 = sb.toString();
            }
            return sb2;
        }

        private static void formatLogEntries(StringBuilder sb2, String[] logEntries) {
            if (logEntries == null || logEntries.length == 0) {
                sb2.append("No detailed logs available.");
                return;
            }
            for (String entry : logEntries) {
                sb2.append(entry).append("\n");
            }
        }
    }

    public ExecutorchRuntimeException(int errorCode, String details) {
        super(ErrorHelper.formatMessage(errorCode, details));
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    /* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/ExecutorchRuntimeException$ExecutorchInvalidArgumentException.class */
    public static class ExecutorchInvalidArgumentException extends IllegalArgumentException {
        private final int errorCode = 18;

        public ExecutorchInvalidArgumentException(String details) {
            super(ErrorHelper.formatMessage(18, details));
            this.errorCode = 18;
        }

        public int getErrorCode() {
            return 18;
        }
    }

    public static RuntimeException makeExecutorchException(int errorCode, String details) {
        switch (errorCode) {
            case INVALID_ARGUMENT /* 18 */:
                return new ExecutorchInvalidArgumentException(details);
            default:
                return new ExecutorchRuntimeException(errorCode, details);
        }
    }
}
