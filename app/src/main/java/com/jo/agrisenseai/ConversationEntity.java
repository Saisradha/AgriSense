package com.jo.agrisenseai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Room entity representing a single chat conversation that belongs to a folder. */
@Entity(tableName = "conversations")
public class ConversationEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long   folderId;
    private String title;
    private String lastMessage;
    private long   createdAt;

    public ConversationEntity(long folderId, String title, String lastMessage, long createdAt) {
        this.folderId    = folderId;
        this.title       = title;
        this.lastMessage = lastMessage;
        this.createdAt   = createdAt;
    }

    public long   getId()                  { return id; }
    public void   setId(long id)           { this.id = id; }

    public long   getFolderId()            { return folderId; }
    public void   setFolderId(long v)      { this.folderId = v; }

    public String getTitle()               { return title; }
    public void   setTitle(String v)       { this.title = v; }

    public String getLastMessage()         { return lastMessage; }
    public void   setLastMessage(String v) { this.lastMessage = v; }

    public long   getCreatedAt()           { return createdAt; }
    public void   setCreatedAt(long v)     { this.createdAt = v; }
}
