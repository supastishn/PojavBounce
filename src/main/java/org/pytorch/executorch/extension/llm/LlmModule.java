package org.pytorch.executorch.extension.llm;

import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import java.io.File;
import org.pytorch.executorch.ExecuTorchRuntime;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/extension/llm/LlmModule.class */
public class LlmModule {
    public static final int MODEL_TYPE_TEXT = 1;
    public static final int MODEL_TYPE_TEXT_VISION = 2;
    public static final int MODEL_TYPE_MULTIMODAL = 2;
    private final HybridData mHybridData;
    private static final int DEFAULT_SEQ_LEN = 128;
    private static final boolean DEFAULT_ECHO = true;

    @DoNotStrip
    private static native HybridData initHybrid(int i, String str, String str2, float f, String str3);

    public native int generate(String str, int i, LlmCallback llmCallback, boolean z);

    private native int appendImagesInput(int[] iArr, int i, int i2, int i3);

    private native int appendNormalizedImagesInput(float[] fArr, int i, int i2, int i3);

    private native int appendAudioInput(byte[] bArr, int i, int i2, int i3);

    private native int appendAudioInputFloat(float[] fArr, int i, int i2, int i3);

    private native int appendRawAudioInput(byte[] bArr, int i, int i2, int i3);

    private native int appendTextInput(String str);

    public native void resetContext();

    @DoNotStrip
    public native void stop();

    @DoNotStrip
    public native int load();

    public LlmModule(int modelType, String modulePath, String tokenizerPath, float temperature, String dataPath) {
        ExecuTorchRuntime.getRuntime();
        File modelFile = new File(modulePath);
        if (!modelFile.canRead() || !modelFile.isFile()) {
            throw new RuntimeException("Cannot load model path " + modulePath);
        }
        File tokenizerFile = new File(tokenizerPath);
        if (!tokenizerFile.canRead() || !tokenizerFile.isFile()) {
            throw new RuntimeException("Cannot load tokenizer path " + tokenizerPath);
        }
        this.mHybridData = initHybrid(modelType, modulePath, tokenizerPath, temperature, dataPath);
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
        return generate(prompt, DEFAULT_SEQ_LEN, llmCallback, true);
    }

    public int generate(String prompt, int seqLen, LlmCallback llmCallback) {
        return generate(null, 0, 0, 0, prompt, seqLen, llmCallback, true);
    }

    public int generate(String prompt, LlmCallback llmCallback, boolean echo) {
        return generate(null, 0, 0, 0, prompt, DEFAULT_SEQ_LEN, llmCallback, echo);
    }

    public int generate(String prompt, LlmGenerationConfig config, LlmCallback llmCallback) {
        int seqLen = config.getSeqLen();
        boolean echo = config.isEcho();
        return generate(null, 0, 0, 0, prompt, seqLen, llmCallback, echo);
    }

    public int generate(int[] image, int width, int height, int channels, String prompt, int seqLen, LlmCallback llmCallback, boolean echo) {
        prefillPrompt(prompt);
        prefillImages(image, width, height, channels);
        return generate("", llmCallback, echo);
    }

    @Experimental
    public long prefillImages(int[] image, int width, int height, int channels) {
        int nativeResult = appendImagesInput(image, width, height, channels);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    @Experimental
    public long prefillImages(float[] image, int width, int height, int channels) {
        int nativeResult = appendNormalizedImagesInput(image, width, height, channels);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    @Experimental
    public long prefillAudio(byte[] audio, int batch_size, int n_bins, int n_frames) {
        int nativeResult = appendAudioInput(audio, batch_size, n_bins, n_frames);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    @Experimental
    public long prefillAudio(float[] audio, int batch_size, int n_bins, int n_frames) {
        int nativeResult = appendAudioInputFloat(audio, batch_size, n_bins, n_frames);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    @Experimental
    public long prefillRawAudio(byte[] audio, int batch_size, int n_channels, int n_samples) {
        int nativeResult = appendRawAudioInput(audio, batch_size, n_channels, n_samples);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }

    @Experimental
    public long prefillPrompt(String prompt) {
        int nativeResult = appendTextInput(prompt);
        if (nativeResult != 0) {
            throw new RuntimeException("Prefill failed with error code: " + nativeResult);
        }
        return 0L;
    }
}
