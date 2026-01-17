/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch.extension.llm;

import org.pytorch.executorch.extension.llm.LlmModuleConfig;

public static class LlmModuleConfig.Builder {
    private String modulePath;
    private String tokenizerPath;
    private float temperature = 0.8f;
    private String dataPath = "";
    private int modelType = 1;

    LlmModuleConfig.Builder() {
    }

    public LlmModuleConfig.Builder modulePath(String modulePath) {
        this.modulePath = modulePath;
        return this;
    }

    public LlmModuleConfig.Builder tokenizerPath(String tokenizerPath) {
        this.tokenizerPath = tokenizerPath;
        return this;
    }

    public LlmModuleConfig.Builder temperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public LlmModuleConfig.Builder dataPath(String dataPath) {
        this.dataPath = dataPath;
        return this;
    }

    public LlmModuleConfig.Builder modelType(int modelType) {
        this.modelType = modelType;
        return this;
    }

    public LlmModuleConfig build() {
        if (this.modulePath == null || this.tokenizerPath == null) {
            throw new IllegalArgumentException("Module path and tokenizer path are required");
        }
        return new LlmModuleConfig(this, null);
    }

    static /* synthetic */ String access$000(LlmModuleConfig.Builder x0) {
        return x0.modulePath;
    }

    static /* synthetic */ String access$100(LlmModuleConfig.Builder x0) {
        return x0.tokenizerPath;
    }

    static /* synthetic */ float access$200(LlmModuleConfig.Builder x0) {
        return x0.temperature;
    }

    static /* synthetic */ String access$300(LlmModuleConfig.Builder x0) {
        return x0.dataPath;
    }

    static /* synthetic */ int access$400(LlmModuleConfig.Builder x0) {
        return x0.modelType;
    }
}
