package com.linghy.model;

@FunctionalInterface
public interface ProgressCallback {
    void onProgress(ProgressUpdate update);
}