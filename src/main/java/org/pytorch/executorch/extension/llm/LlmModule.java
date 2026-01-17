/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.facebook.jni.HybridData
 *  com.facebook.jni.annotations.DoNotStrip
 */
package org.pytorch.executorch.extension.llm;

import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import java.io.File;
import org.pytorch.executorch.ExecuTorchRuntime;
import org.pytorch.executorch.annotations.Experimental;
import org.pytorch.executorch.extension.llm.LlmCallback;
import org.pytorch.executorch.extension.llm.LlmGenerationConfig;
import org.pytorch.executorch.extension.llm.LlmModuleConfig;

@Experimental
public class LlmModule {
    public static final int MODEL_TYPE_TEXT = 1;
    public static final int MODEL_TYPE_TEXT_VISION = 2;
    public static final int MODEL_TYPE_MULTIMODAL = 2;
    private final HybridData mHybridData;
    private static final int DEFAULT_SEQ_LEN = 128;
    private static final boolean DEFAULT_ECHO = true;

    @DoNotStrip
    private static native HybridData initHybrid(int var0, String var1, String var2, float var3, String var4);

    public LlmModule(int modelType, String modulePath, String tokenizerPath, float temperature, String dataPath) {
        ExecuTorchRuntime runtime = ExecuTorchRuntime.getRuntime();
        File modelFile = new File(modulePath);
        if (!modelFile.canRead() || !modelFile.isFile()) {
            throw new RuntimeException("Cannot load model path " + modulePath);
        }
        File tokenizerFile = new File(tokenizerPath);
        if (!tokenizerFile.canRead() || !tokenizerFile.isFile()) {
            throw new RuntimeException("Cannot load tokenizer path " + tokenizerPath);
        }
        this.mHybridData = LlmModule.initHybrid(modelType, modulePath, tokenizerPath, temperature, dataPath);
    }

    public LlmModule(String modulePath, String tokenizerPath, float temperature) {
        this(1, modulePath, tokenizerPath, temperature, null);
    }

    public LlmModule(String modulePath, String tokenizerPath, float temperature, String dataPath) {
        this(1, modulePath, tokenizerPath, temperature, dataPath);
    }

    public LlmModule(int modelType, String modulePath, String tokenizerPath, float temperature) {
        this(modelType, modulePath, tokenizerPath, temperature, null);
    }

    public LlmModule(LlmModuleConfig config) {
        this(config.getModelType(), config.getModulePath(), config.getTokenizerPath(), config.getTemperature(), config.getDataPath());
    }

    public void resetNative() {
        this.mHybridData.resetNative();
    }

    public int generate(String prompt, LlmCallback llmCallback) {
        return this.generate(prompt, 128, llmCallback, true);
    }

    public int generate(String prompt, int seqLen, LlmCallback llmCallback) {
        return this.generate(null, 0, 0, 0, prompt, seqLen, llmCallback, true);
    }

    public int generate(String prompt, LlmCallback llmCallback, boolean echo) {
        return this.generate(null, 0, 0, 0, prompt, 128, llmCallback, echo);
    }

    public native int generate(String var1, int var2, LlmCallback var3, boolean var4);

    public int generate(String prompt, LlmGenerationConfig config, LlmCallback llmCallback) {
        int seqLen = config.getSeqLen();
        boolean echo = config.isEcho();
        return this.generate(null, 0, 0, 0, prompt, seqLen, llmCallback, echo);
    }

    public int generate(int[] image, int width, int height, int channels, String prompt, int seqLen, LlmCallback llmCallback, boolean echo) {
        this.prefillPrompt(prompt);
        this.prefillImages(image, width, height, channels);
        return this.generate("", llmCallback, echo);
    }

    @Experimental
    public long prefillImages(int[] image, int width, int height, int channels) {
        int nativeResult = this.appendImagesInput(image, width, height, channels);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    private native int appendImagesInput(int[] var1, int var2, int var3, int var4);

    @Experimental
    public long prefillImages(float[] image, int width, int height, int channels) {
        int nativeResult = this.appendNormalizedImagesInput(image, width, height, channels);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    private native int appendNormalizedImagesInput(float[] var1, int var2, int var3, int var4);

    @Experimental
    public long prefillAudio(byte[] audio, int batch_size, int n_bins, int n_frames) {
        int nativeResult = this.appendAudioInput(audio, batch_size, n_bins, n_frames);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    private native int appendAudioInput(byte[] var1, int var2, int var3, int var4);

    @Experimental
    public long prefillAudio(float[] audio, int batch_size, int n_bins, int n_frames) {
        int nativeResult = this.appendAudioInputFloat(audio, batch_size, n_bins, n_frames);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    private native int appendAudioInputFloat(float[] var1, int var2, int var3, int var4);

    @Experimental
    public long prefillRawAudio(byte[] audio, int batch_size, int n_channels, int n_samples) {
        int nativeResult = this.appendRawAudioInput(audio, batch_size, n_channels, n_samples);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    private native int appendRawAudioInput(byte[] var1, int var2, int var3, int var4);

    @Experimental
    public long prefillPrompt(String prompt) {
        int nativeResult = this.appendTextInput(prompt);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    private native int appendTextInput(String var1);

    public native void resetContext();

    @DoNotStrip
    public native void stop();

    @DoNotStrip
    public native int load();
}
