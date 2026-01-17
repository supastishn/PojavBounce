package org.pytorch.executorch;

/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/MethodMetadata.class */
public class MethodMetadata {
    private String mName;
    private String[] mBackends;

    MethodMetadata setName(String name) {
        this.mName = name;
        return this;
    }

    public String getName() {
        return this.mName;
    }

    MethodMetadata setBackends(String[] backends) {
        this.mBackends = backends;
        return this;
    }

    public String[] getBackends() {
        return this.mBackends;
    }
}
