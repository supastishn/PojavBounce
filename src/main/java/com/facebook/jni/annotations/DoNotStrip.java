/*
 * Stub implementation of Facebook's DoNotStrip annotation.
 * This provides API compatibility without requiring the actual Facebook JNI library.
 */
package com.facebook.jni.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to prevent ProGuard/R8 from stripping annotated elements.
 * This stub allows ExecuTorch Java code to compile without the actual Facebook JNI dependency.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface DoNotStrip {
}
