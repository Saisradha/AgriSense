package com.jo.agrisenseai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert
    long insert(ConversationEntity conversation);

    @Query("SELECT * FROM conversations WHERE folderId = :folderId ORDER BY createdAt DESC")
    List<ConversationEntity> getByFolder(long folderId);

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    ConversationEntity findById(long id);

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    void updateTitle(long id, String title);

    @Query("UPDATE conversations SET lastMessage = :lastMessage WHERE id = :id")
    void updateLastMessage(long id, String lastMessage);

    @Query("UPDATE conversations SET folderId = :newFolderId WHERE id = :id")
    void moveToFolder(long id, long newFolderId);

    /** Bulk-move all conversations from a deleted folder to General. */
    @Query("UPDATE conversations SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    void moveBulkToFolder(long oldFolderId, long newFolderId);

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT COUNT(*) FROM conversations WHERE folderId = :folderId")
    int countByFolder(long folderId);

    @Query("SELECT DISTINCT c.* FROM conversations c " +
           "LEFT JOIN chat_history m ON c.id = m.conversationId " +
           "WHERE c.title LIKE :query OR m.text LIKE :query " +
           "ORDER BY c.createdAt DESC")
    List<ConversationEntity> searchConversations(String query);

    @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
    List<ConversationEntity> getAllConversations();
}
