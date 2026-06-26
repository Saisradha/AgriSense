package com.jo.agrisenseai;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = {ChatEntity.class, FolderEntity.class, ConversationEntity.class},
    version  = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ChatDao         chatDao();
    public abstract FolderDao       folderDao();
    public abstract ConversationDao conversationDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "agrisense_database")
                    .fallbackToDestructiveMigration()   // version 1→2: wipe & recreate
                    .build();
        }
        return instance;
    }
}
