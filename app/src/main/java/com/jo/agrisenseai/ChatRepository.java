package com.jo.agrisenseai;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for chat message persistence.
 * All operations are conversation-scoped in the new v2 schema.
 */
public class ChatRepository {

    private final ChatDao chatDao;
    private final ExecutorService executor;

    public interface Callback<T> {
        void onComplete(T result);
    }

    public ChatRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.chatDao  = db.chatDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /** Insert a message (must include a valid conversationId). */
    public void insert(final ChatEntity entity) {
        executor.execute(() -> chatDao.insert(entity));
    }

    /** Load all messages for a given conversation, ordered by timestamp. */
    public void getMessagesForConversation(long conversationId, Callback<List<ChatEntity>> callback) {
        executor.execute(() -> {
            List<ChatEntity> messages = chatDao.getByConversation(conversationId);
            if (callback != null) callback.onComplete(messages);
        });
    }

    /** Delete all messages in a single conversation (clears the chat; keeps conversation entity). */
    public void clearConversationHistory(long conversationId, Callback<Void> callback) {
        executor.execute(() -> {
            chatDao.deleteByConversationId(conversationId);
            if (callback != null) callback.onComplete(null);
        });
    }

    /** Delete all messages across ALL conversations (admin use only). */
    public void clearAllHistory(Callback<Void> callback) {
        executor.execute(() -> {
            chatDao.clearHistory();
            if (callback != null) callback.onComplete(null);
        });
    }

    /** Delete specific messages by timestamp (multi-select deletion). */
    public void deleteSelectedMessages(List<Long> timestamps, Callback<Void> callback) {
        executor.execute(() -> {
            chatDao.deleteByTimestamps(timestamps);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) executor.shutdown();
    }
}
