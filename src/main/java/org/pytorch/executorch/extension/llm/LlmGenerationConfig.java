/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch.extension.llm;

public class LlmGenerationConfig {
    private final boolean echo;
    private final int maxNewTokens;
    private final boolean warming;
    private final int seqLen;
    private final float temperature;

    private LlmGenerationConfig(Builder builder) {
        this.echo = builder.echo;
        this.maxNewTokens = builder.maxNewTokens;
        this.warming = builder.warming;
        this.seqLen = builder.seqLen;
        this.temperature = builder.temperature;
    }

    public static Builder create() {
        return new Builder();
    }

    public boolean isEcho() {
        return this.echo;
    }

    public int getMaxNewTokens() {
        return this.maxNewTokens;
    }

    public boolean isWarming() {
        return this.warming;
    }

    public int getSeqLen() {
        return this.seqLen;
    }

    public float getTemperature() {
        return this.temperature;
    }

    public static class Builder {
        private boolean echo = true;
        private int maxNewTokens = -1;
        private boolean warming = false;
        private int seqLen = -1;
        private float temperature = 0.8f;

        Builder() {
        }

        public Builder echo(boolean echo) {
            this.echo = echo;
            return this;
        }

        public Builder maxNewTokens(int maxNewTokens) {
            this.maxNewTokens = maxNewTokens;
            return this;
        }

        public Builder warming(boolean warming) {
            this.warming = warming;
            return this;
        }

        public Builder seqLen(int seqLen) {
            this.seqLen = seqLen;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public LlmGenerationConfig build() {
            return new LlmGenerationConfig(this);
        }
    }
}
