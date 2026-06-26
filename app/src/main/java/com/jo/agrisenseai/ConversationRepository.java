package com.jo.agrisenseai;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing folders and conversations (chat threads).
 * All operations run on a single background thread and return results via callbacks.
 */
public class ConversationRepository {

    private final FolderDao       folderDao;
    private final ConversationDao conversationDao;
    private final ChatDao         chatDao;
    private final ExecutorService executor;

    public interface Callback<T> {
        void onComplete(T result);
    }

    public ConversationRepository(Context context) {
        AppDatabase db    = AppDatabase.getInstance(context);
        this.folderDao       = db.folderDao();
        this.conversationDao = db.conversationDao();
        this.chatDao         = db.chatDao();
        this.executor        = Executors.newSingleThreadExecutor();
    }

    // ── Initialization ─────────────────────────────────────────────────────────

    /**
     * Ensures the "General" folder exists and at least one conversation exists within it.
     * Callback returns {@code long[]{generalFolderId, conversationId}}.
     */
    public void ensureDefaultData(Callback<long[]> callback) {
        executor.execute(() -> {
            // Get or create General folder
            FolderEntity general = folderDao.findByName("General");
            long generalId;
            if (general == null) {
                FolderEntity f = new FolderEntity("General", System.currentTimeMillis());
                generalId = folderDao.insert(f);
            } else {
                generalId = general.getId();
            }

            // Get or create first conversation
            List<ConversationEntity> convs = conversationDao.getByFolder(generalId);
            long defaultConvId;
            if (convs.isEmpty()) {
                ConversationEntity conv = new ConversationEntity(
                        generalId, "New Chat", "", System.currentTimeMillis());
                defaultConvId = conversationDao.insert(conv);
            } else {
                defaultConvId = convs.get(0).getId();
            }

            if (callback != null) callback.onComplete(new long[]{generalId, defaultConvId});
        });
    }

    // ── Conversation operations ────────────────────────────────────────────────

    public void insertConversation(long folderId, String title, Callback<Long> callback) {
        executor.execute(() -> {
            ConversationEntity conv = new ConversationEntity(
                    folderId, title, "", System.currentTimeMillis());
            long id = conversationDao.insert(conv);
            if (callback != null) callback.onComplete(id);
        });
    }

    public void getConversationsByFolder(long folderId, Callback<List<ConversationEntity>> callback) {
        executor.execute(() -> {
            List<ConversationEntity> convs = conversationDao.getByFolder(folderId);
            if (callback != null) callback.onComplete(convs);
        });
    }

    public void renameConversation(long id, String newTitle, Callback<Void> callback) {
        executor.execute(() -> {
            conversationDao.updateTitle(id, newTitle);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void moveConversation(long id, long newFolderId, Callback<Void> callback) {
        executor.execute(() -> {
            conversationDao.moveToFolder(id, newFolderId);
            if (callback != null) callback.onComplete(null);
        });
    }

    /** Permanently deletes a conversation and ALL its chat messages. */
    public void deleteConversation(long id, Callback<Void> callback) {
        executor.execute(() -> {
            chatDao.deleteByConversationId(id);
            conversationDao.deleteById(id);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void updateConversationTitle(long id, String title, Callback<Void> callback) {
        executor.execute(() -> {
            conversationDao.updateTitle(id, title);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void updateConversationLastMessage(long id, String lastMessage, Callback<Void> callback) {
        executor.execute(() -> {
            conversationDao.updateLastMessage(id, lastMessage);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void searchConversations(String query, Callback<List<ConversationEntity>> callback) {
        executor.execute(() -> {
            List<ConversationEntity> results = conversationDao.searchConversations("%" + query + "%");
            if (callback != null) callback.onComplete(results);
        });
    }

    public void getAllConversations(Callback<List<ConversationEntity>> callback) {
        executor.execute(() -> {
            List<ConversationEntity> list = conversationDao.getAllConversations();
            if (callback != null) callback.onComplete(list);
        });
    }

    public void shutdown() {
        if (!executor.isShutdown()) executor.shutdown();
    }
}
