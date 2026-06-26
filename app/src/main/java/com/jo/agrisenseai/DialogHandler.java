package com.jo.agrisenseai;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class DialogHandler {

    public interface RenameCallback {
        /**
         * Called when user presses Save/Create.
         * @param newName The entered text.
         * @return The error message if validation fails, or null if valid (closes the dialog).
         */
        String onRename(String newName);
    }

    public interface DeleteConfirmCallback {
        void onDeleteConfirm();
    }

    public interface FolderDeleteCallback {
        void onMoveToGeneral();
        void onDeleteAll();
    }

    public static void showRenameDialog(Context context, String title, String currentName, String hint, RenameCallback callback) {
        EditText input = new EditText(context);
        if (currentName != null) {
            input.setText(currentName);
            input.setSelection(currentName.length());
        }
        if (hint != null) {
            input.setHint(hint);
        }

        // Add margins
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginHorizontal = (int) (24 * context.getResources().getDisplayMetrics().density);
        int marginVertical = (int) (8 * context.getResources().getDisplayMetrics().density);
        lp.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
        input.setLayoutParams(lp);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String text = input.getText().toString().trim();
                if (callback != null) {
                    String error = callback.onRename(text);
                    if (error != null) {
                        input.setError(error);
                        return;
                    }
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    public static void showDeleteChatConfirmation(Context context, String chatTitle, DeleteConfirmCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Chat")
                .setMessage("Are you sure you want to delete this chat?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (callback != null) callback.onDeleteConfirm();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showDeleteFolderConfirmation(Context context, String folderName, FolderDeleteCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Folder")
                .setMessage("What would you like to do with the chats inside this folder?")
                .setPositiveButton("Move chats to General", (d, w) -> {
                    if (callback != null) callback.onMoveToGeneral();
                })
                .setNeutralButton("Delete folder and all chats", (d, w) -> {
                    if (callback != null) callback.onDeleteAll();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
