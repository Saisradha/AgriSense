package com.jo.agrisenseai;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;

public class PopupMenuHandler {

    public interface ConversationMenuListener {
        void onOpen(ConversationEntity conv);
        void onRename(ConversationEntity conv);
        void onDelete(ConversationEntity conv);
    }

    public interface FolderMenuListener {
        void onRename(FolderEntity folder);
        void onDelete(FolderEntity folder);
    }

    public static void showConversationMenu(Context context, View anchor, ConversationEntity conv, ConversationMenuListener listener) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, "Open");
        popup.getMenu().add(0, 2, 1, "Rename");
        popup.getMenu().add(0, 3, 2, "Delete");
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                if (listener != null) listener.onOpen(conv);
                return true;
            } else if (id == 2) {
                if (listener != null) listener.onRename(conv);
                return true;
            } else if (id == 3) {
                if (listener != null) listener.onDelete(conv);
                return true;
            }
            return false;
        });
        popup.show();
    }

    public static void showFolderMenu(Context context, View anchor, FolderEntity folder, FolderMenuListener listener) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, "Rename Folder");
        // Do not allow deleting the General folder
        if (!"General".equals(folder.getName())) {
            popup.getMenu().add(0, 2, 1, "Delete Folder");
        }
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                if (listener != null) listener.onRename(folder);
                return true;
            } else if (id == 2) {
                if (listener != null) listener.onDelete(folder);
                return true;
            }
            return false;
        });
        popup.show();
    }
}
