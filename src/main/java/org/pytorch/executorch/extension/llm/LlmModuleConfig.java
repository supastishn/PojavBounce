package org.pytorch.executorch.extension.llm;

/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/extension/llm/LlmModuleConfig.class */
public class LlmModuleConfig {
    private final String modulePath;
    private final String tokenizerPath;
    private final float temperature;
    private final String dataPath;
    private final int modelType;
    public static final int MODEL_TYPE_TEXT = 1;
    public static final int MODEL_TYPE_TEXT_VISION = 2;
    public static final int MODEL_TYPE_MULTIMODAL = 2;

    private LlmModuleConfig(Builder builder) {
        this.modulePath = builder.modulePath;
        this.tokenizerPath = builder.tokenizerPath;
        this.temperature = builder.temperature;
        this.dataPath = builder.dataPath;
        this.modelType = builder.modelType;
    }

    public static Builder create() {
        return new Builder();
    }

    public String getModulePath() {
        return this.modulePath;
    }

    public String getTokenizerPath() {
        return this.tokenizerPath;
    }

    public float getTemperature() {
        return this.temperature;
    }

    public String getDataPath() {
        return this.dataPath;
    }

    public int getModelType() {
        return this.modelType;
    }

    /* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/extension/llm/LlmModuleConfig$Builder.class */
    public static class Builder {
        private String modulePath;
        private String tokenizerPath;
        private float temperature = 0.8f;
        private String dataPath = "";
        private int modelType = 1;

        Builder() {
        }

        public Builder modulePath(String modulePath) {
            this.modulePath = modulePath;
            return this;
        }

        public Builder tokenizerPath(String tokenizerPath) {
            this.tokenizerPath = tokenizerPath;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder dataPath(String dataPath) {
            this.dataPath = dataPath;
            return this;
        }

        public Builder modelType(int modelType) {
            this.modelType = modelType;
            return this;
        }

        public LlmModuleConfig build() {
            if (this.modulePath == null || this.tokenizerPath == null) {
                throw new IllegalArgumentException("Module path and tokenizer path are required");
            }
            return new LlmModuleConfig(this);
        }
    }
}
