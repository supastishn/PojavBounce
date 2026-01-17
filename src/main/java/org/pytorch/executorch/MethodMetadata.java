/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

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
