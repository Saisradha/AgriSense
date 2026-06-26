package com.jo.agrisenseai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Room entity representing a folder that groups conversations. */
@Entity(tableName = "folders")
public class FolderEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private long createdAt;

    public FolderEntity(String name, long createdAt) {
        this.name      = name;
        this.createdAt = createdAt;
    }

    public long getId()            { return id; }
    public void setId(long id)     { this.id = id; }

    public String getName()               { return name; }
    public void   setName(String name)    { this.name = name; }

    public long getCreatedAt()            { return createdAt; }
    public void setCreatedAt(long v)      { this.createdAt = v; }
}
