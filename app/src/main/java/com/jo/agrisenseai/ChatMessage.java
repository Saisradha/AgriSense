package com.jo.agrisenseai;

import java.util.List;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_CHECKLIST = 2;

    private String text;
    private final int type;
    private final long timestamp;
    private boolean isLoading;
    private List<String> checklistItems;

    public ChatMessage(String text, int type) {
        this(text, type, false, null);
    }

    public ChatMessage(String text, int type, boolean isLoading) {
        this(text, type, isLoading, null);
    }

    public ChatMessage(String text, int type, boolean isLoading, List<String> checklistItems) {
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.isLoading = isLoading;
        this.checklistItems = checklistItems;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    public List<String> getChecklistItems() {
        return checklistItems;
    }

    public void setChecklistItems(List<String> checklistItems) {
        this.checklistItems = checklistItems;
    }
}
