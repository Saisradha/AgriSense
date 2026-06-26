package com.jo.agrisenseai;

public interface AIServiceCallback {
    void onSuccess(String responseText);
    void onFailure(String errorMessage);
}
