package com.jo.agrisenseai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FolderDao {

    @Insert
    long insert(FolderEntity folder);

    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    List<FolderEntity> getAllFolders();

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    FolderEntity findByName(String name);

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    void updateName(long id, String name);

    @Query("DELETE FROM folders WHERE id = :id")
    void deleteById(long id);
}
