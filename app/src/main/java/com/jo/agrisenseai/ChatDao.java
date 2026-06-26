package com.jo.agrisenseai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    void insert(ChatEntity entity);

    /** Legacy: returns all messages sorted by time. */
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    List<ChatEntity> getAllMessages();

    /** Returns messages belonging to a specific conversation. */
    @Query("SELECT * FROM chat_history WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    List<ChatEntity> getByConversation(long conversationId);

    /** Deletes ALL messages (nuclear option — used only by admin/testing). */
    @Query("DELETE FROM chat_history")
    void clearHistory();

    /** Deletes all messages in a single conversation. */
    @Query("DELETE FROM chat_history WHERE conversationId = :conversationId")
    void deleteByConversationId(long conversationId);

    /** Deletes specific messages by their timestamps (multi-select deletion). */
    @Query("DELETE FROM chat_history WHERE timestamp IN (:timestamps)")
    void deleteByTimestamps(List<Long> timestamps);
}
