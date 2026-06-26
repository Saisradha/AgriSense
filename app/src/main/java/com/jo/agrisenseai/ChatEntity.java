package com.jo.agrisenseai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_history")
public class ChatEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String text;
    private int    type;
    private long   timestamp;
    private String checklistJson;
    /** Links this message to a specific conversation. 0 = legacy/unassigned. */
    private long   conversationId;

    public ChatEntity(String text, int type, long timestamp, String checklistJson, long conversationId) {
        this.text           = text;
        this.type           = type;
        this.timestamp      = timestamp;
        this.checklistJson  = checklistJson;
        this.conversationId = conversationId;
    }

    public int    getId()                     { return id; }
    public void   setId(int id)               { this.id = id; }

    public String getText()                   { return text; }
    public void   setText(String text)        { this.text = text; }

    public int    getType()                   { return type; }
    public void   setType(int type)           { this.type = type; }

    public long   getTimestamp()              { return timestamp; }
    public void   setTimestamp(long v)        { this.timestamp = v; }

    public String getChecklistJson()          { return checklistJson; }
    public void   setChecklistJson(String v)  { this.checklistJson = v; }

    public long   getConversationId()         { return conversationId; }
    public void   setConversationId(long v)   { this.conversationId = v; }
}
