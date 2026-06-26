package com.jo.agrisenseai;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderRepository {

    private final FolderDao folderDao;
    private final ConversationDao conversationDao;
    private final ChatDao chatDao;
    private final ExecutorService executor;

    public interface Callback<T> {
        void onComplete(T result);
    }

    public FolderRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.folderDao = db.folderDao();
        this.conversationDao = db.conversationDao();
        this.chatDao = db.chatDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void insertFolder(String name, Callback<Long> callback) {
        executor.execute(() -> {
            FolderEntity f = new FolderEntity(name, System.currentTimeMillis());
            long id = folderDao.insert(f);
            if (callback != null) callback.onComplete(id);
        });
    }

    public void getAllFolders(Callback<List<FolderEntity>> callback) {
        executor.execute(() -> {
            List<FolderEntity> folders = folderDao.getAllFolders();
            if (callback != null) callback.onComplete(folders);
        });
    }

    public void renameFolder(long id, String newName, Callback<Void> callback) {
        executor.execute(() -> {
            folderDao.updateName(id, newName);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void deleteFolder(long folderId, Callback<Void> callback) {
        executor.execute(() -> {
            FolderEntity general = folderDao.findByName("General");
            if (general != null && general.getId() != folderId) {
                conversationDao.moveBulkToFolder(folderId, general.getId());
            }
            folderDao.deleteById(folderId);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void deleteFolderPermanently(long folderId, Callback<Void> callback) {
        executor.execute(() -> {
            List<ConversationEntity> convs = conversationDao.getByFolder(folderId);
            for (ConversationEntity conv : convs) {
                chatDao.deleteByConversationId(conv.getId());
                conversationDao.deleteById(conv.getId());
            }
            folderDao.deleteById(folderId);
            if (callback != null) callback.onComplete(null);
        });
    }

    public void countConversationsInFolder(long folderId, Callback<Integer> callback) {
        executor.execute(() -> {
            int count = conversationDao.countByFolder(folderId);
            if (callback != null) callback.onComplete(count);
        });
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) executor.shutdown();
    }
}
