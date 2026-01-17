/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch.extension.llm;

import org.pytorch.executorch.extension.llm.LlmGenerationConfig;

public static class LlmGenerationConfig.Builder {
    private boolean echo = true;
    private int maxNewTokens = -1;
    private boolean warming = false;
    private int seqLen = -1;
    private float temperature = 0.8f;

    LlmGenerationConfig.Builder() {
    }

    public LlmGenerationConfig.Builder echo(boolean echo) {
        this.echo = echo;
        return this;
    }

    public LlmGenerationConfig.Builder maxNewTokens(int maxNewTokens) {
        this.maxNewTokens = maxNewTokens;
        return this;
    }

    public LlmGenerationConfig.Builder warming(boolean warming) {
        this.warming = warming;
        return this;
    }

    public LlmGenerationConfig.Builder seqLen(int seqLen) {
        this.seqLen = seqLen;
        return this;
    }

    public LlmGenerationConfig.Builder temperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public LlmGenerationConfig build() {
        return new LlmGenerationConfig(this, null);
    }

    static /* synthetic */ boolean access$000(LlmGenerationConfig.Builder x0) {
        return x0.echo;
    }

    static /* synthetic */ int access$100(LlmGenerationConfig.Builder x0) {
        return x0.maxNewTokens;
    }

    static /* synthetic */ boolean access$200(LlmGenerationConfig.Builder x0) {
        return x0.warming;
    }

    static /* synthetic */ int access$300(LlmGenerationConfig.Builder x0) {
        return x0.seqLen;
    }

    static /* synthetic */ float access$400(LlmGenerationConfig.Builder x0) {
        return x0.temperature;
    }
}
