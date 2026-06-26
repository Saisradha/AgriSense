package com.jo.agrisenseai;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Browse folders and conversations (ChatGPT-style).
 *
 * Two screens in one activity:
 *   SCREEN 1 — Folder list  (toolbar title = "Conversations", FAB = New Folder)
 *   SCREEN 2 — Conversation list  (toolbar title = folder name, FAB = New Chat)
 *
 * Returns RESULT_OK with extra:
 *   "conversationId" (long) — the conversation to open in AIAssistantFragment
 *   "conversationDeleted" (boolean) — true if the current conversation was deleted without picking another
 */
public class ConversationBrowserActivity extends AppCompatActivity {

    public static final String EXTRA_CURRENT_CONV_ID  = "currentConversationId";
    public static final String RESULT_CONV_ID         = "conversationId";
    public static final String RESULT_CONV_DELETED    = "conversationDeleted";

    // ── Views ──────────────────────────────────────────────────────────────────
    private MaterialToolbar        toolbar;
    private RecyclerView           recyclerView;
    private FloatingActionButton   fab;
    private LinearLayout           emptyState;
    private TextView               emptyText;

    // ── Data ───────────────────────────────────────────────────────────────────
    private ConversationRepository  repo;
    private FolderRepository        folderRepo;
    private FolderAdapter           folderAdapter;
    private ConversationListAdapter convAdapter;

    private final List<FolderEntity>      folders       = new ArrayList<>();
    private final List<ConversationEntity> conversations = new ArrayList<>();

    private boolean      isInFolderList = true;
    private FolderEntity currentFolder  = null;
    private long         currentConvId  = -1;   // conversation currently open in AI fragment
    private boolean      currentConvWasDeleted = false;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_browser);

        currentConvId = getIntent().getLongExtra(EXTRA_CURRENT_CONV_ID, -1);

        toolbar      = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        fab          = findViewById(R.id.fab);
        emptyState   = findViewById(R.id.emptyState);
        emptyText    = findViewById(R.id.emptyText);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        repo = new ConversationRepository(this);
        folderRepo = new FolderRepository(this);

        setupAdapters();
        showFolderList();
    }

    @Override
    public void onBackPressed() {
        if (!isInFolderList) {
            showFolderList();
        } else {
            // Return to fragment — if current conversation was deleted, signal it
            if (currentConvWasDeleted) {
                Intent result = new Intent();
                result.putExtra(RESULT_CONV_DELETED, true);
                setResult(RESULT_OK, result);
            }
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repo != null) repo.shutdown();
        if (folderRepo != null) folderRepo.shutdown();
    }

    // ── Screens ────────────────────────────────────────────────────────────────

    private void showFolderList() {
        isInFolderList = true;
        currentFolder  = null;

        toolbar.setTitle("Conversations");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);

        fab.setOnClickListener(v -> showCreateFolderDialog());
        emptyText.setText("No folders yet.\nTap + to create one.");

        folderRepo.getAllFolders(folderList -> runOnUiThread(() -> {
            folders.clear();
            folders.addAll(folderList);
            recyclerView.setAdapter(folderAdapter);
            folderAdapter.notifyDataSetChanged();
            emptyState.setVisibility(folders.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    private void showConversationList(FolderEntity folder) {
        isInFolderList = false;
        currentFolder  = folder;

        toolbar.setTitle(folder.getName());
        toolbar.setNavigationIcon(android.R.drawable.ic_media_previous);
        toolbar.setNavigationOnClickListener(v -> showFolderList());

        fab.setOnClickListener(v -> createNewConversation(folder.getId()));
        emptyText.setText("No chats in this folder.\nTap + to start a new chat.");

        repo.getConversationsByFolder(folder.getId(), convList -> runOnUiThread(() -> {
            conversations.clear();
            conversations.addAll(convList);
            recyclerView.setAdapter(convAdapter);
            convAdapter.notifyDataSetChanged();
            emptyState.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private void setupAdapters() {
        folderAdapter = new FolderAdapter(folders, new FolderAdapter.FolderListener() {
            @Override public void onFolderClick(FolderEntity folder) {
                showConversationList(folder);
            }
            @Override public void onFolderLongClick(View anchor, FolderEntity folder) {
                showFolderContextMenu(anchor, folder);
            }
        });

        convAdapter = new ConversationListAdapter(conversations, new ConversationListAdapter.ConversationListener() {
            @Override public void onConversationClick(ConversationEntity conv) {
                returnWithConversation(conv.getId());
            }
            @Override public void onConversationLongClick(View anchor, ConversationEntity conv) {
                showConversationContextMenu(anchor, conv);
            }
        });
    }

    // ── Result helpers ─────────────────────────────────────────────────────────

    private void returnWithConversation(long conversationId) {
        Intent result = new Intent();
        result.putExtra(RESULT_CONV_ID, conversationId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void createNewConversation(long folderId) {
        repo.insertConversation(folderId, "New Chat", id -> {
            runOnUiThread(() -> returnWithConversation(id));
        });
    }

    // ── Folder context menu ────────────────────────────────────────────────────

    private void showFolderContextMenu(View anchor, FolderEntity folder) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Rename Folder");
        if (!"General".equals(folder.getName())) {
            popup.getMenu().add(0, 2, 1, "🗑️  Delete Folder");
        }
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { showRenameFolderDialog(folder);    return true; }
            if (id == 2) { showDeleteFolderDialog(folder);    return true; }
            return false;
        });
        popup.show();
    }

    private void showCreateFolderDialog() {
        EditText input = buildEditText(null, "Folder Name");
        View dialogView = buildDialogView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create Folder")
                .setView(dialogView)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button createBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            createBtn.setOnClickListener(v -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    input.setError("Folder name cannot be empty");
                    return;
                }
                boolean exists = false;
                for (FolderEntity f : folders) {
                    if (f.getName().equalsIgnoreCase(name)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    input.setError("Folder already exists");
                    return;
                }
                folderRepo.insertFolder(name, id -> runOnUiThread(() -> {
                    dialog.dismiss();
                    showFolderList();
                }));
            });
        });

        dialog.show();
    }

    private void showRenameFolderDialog(FolderEntity folder) {
        EditText input = buildEditText(folder.getName(), "Folder Name");
        View dialogView = buildDialogView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rename Folder")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    input.setError("Folder name cannot be empty");
                    return;
                }
                boolean exists = false;
                for (FolderEntity f : folders) {
                    if (f.getId() != folder.getId() && f.getName().equalsIgnoreCase(name)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    input.setError("Folder already exists");
                    return;
                }
                folderRepo.renameFolder(folder.getId(), name, unused -> runOnUiThread(() -> {
                    dialog.dismiss();
                    folder.setName(name);
                    int pos = folders.indexOf(folder);
                    if (pos >= 0) folderAdapter.notifyItemChanged(pos);
                }));
            });
        });

        dialog.show();
    }

    private void showDeleteFolderDialog(FolderEntity folder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage("Move chats to General or permanently delete them?")
                .setPositiveButton("Move to General", (d, w) ->
                        folderRepo.deleteFolder(folder.getId(), unused -> runOnUiThread(() -> {
                            int pos = folders.indexOf(folder);
                            if (pos >= 0) {
                                folders.remove(pos);
                                folderAdapter.notifyItemRemoved(pos);
                            }
                            emptyState.setVisibility(folders.isEmpty() ? View.VISIBLE : View.GONE);
                            Toast.makeText(this, "Folder deleted. Chats moved to General.", Toast.LENGTH_SHORT).show();
                        })))
                .setNeutralButton("Permanently Delete", (d, w) ->
                        repo.getConversationsByFolder(folder.getId(), convList -> {
                            boolean containsCurrent = false;
                            for (ConversationEntity conv : convList) {
                                if (conv.getId() == currentConvId) {
                                    containsCurrent = true;
                                    break;
                                }
                            }
                            final boolean finalContainsCurrent = containsCurrent;
                            folderRepo.deleteFolderPermanently(folder.getId(), unused -> runOnUiThread(() -> {
                                int pos = folders.indexOf(folder);
                                if (pos >= 0) {
                                    folders.remove(pos);
                                    folderAdapter.notifyItemRemoved(pos);
                                }
                                emptyState.setVisibility(folders.isEmpty() ? View.VISIBLE : View.GONE);
                                if (finalContainsCurrent) {
                                    currentConvWasDeleted = true;
                                }
                                Toast.makeText(this, "Folder and all its chats permanently deleted.", Toast.LENGTH_SHORT).show();
                            }));
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Conversation context menu ──────────────────────────────────────────────

    private void showConversationContextMenu(View anchor, ConversationEntity conv) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "✏️  Rename");
        popup.getMenu().add(0, 2, 1, "Move to Folder");
        popup.getMenu().add(0, 3, 2, "🗑️  Delete Chat");
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { showRenameConversationDialog(conv);    return true; }
            if (id == 2) { showMoveConversationDialog(conv);      return true; }
            if (id == 3) { showDeleteConversationDialog(conv);    return true; }
            return false;
        });
        popup.show();
    }

    private void showRenameConversationDialog(ConversationEntity conv) {
        EditText input = buildEditText(conv.getTitle(), "Chat Title");
        View dialogView = buildDialogView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rename Chat")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String title = input.getText().toString().trim();
                if (title.isEmpty()) {
                    input.setError("Title cannot be empty");
                    return;
                }
                repo.renameConversation(conv.getId(), title, unused -> runOnUiThread(() -> {
                    dialog.dismiss();
                    conv.setTitle(title);
                    int pos = conversations.indexOf(conv);
                    if (pos >= 0) convAdapter.notifyItemChanged(pos);
                }));
            });
        });

        dialog.show();
    }

    private void showMoveConversationDialog(ConversationEntity conv) {
        folderRepo.getAllFolders(folderList -> runOnUiThread(() -> {
            // Display all folders (do not filter out current folder)
            List<FolderEntity> targets = new ArrayList<>(folderList);
            if (targets.isEmpty()) {
                Toast.makeText(this, "No folders available.", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[targets.size()];
            for (int i = 0; i < targets.size(); i++) names[i] = targets.get(i).getName();

            new AlertDialog.Builder(this)
                    .setTitle("Move to Folder")
                    .setItems(names, (d, which) -> {
                        FolderEntity target = targets.get(which);
                        repo.moveConversation(conv.getId(), target.getId(), unused -> runOnUiThread(() -> {
                            // If we moved it to a different folder, remove it from the current display list immediately
                            if (target.getId() != conv.getFolderId()) {
                                int pos = conversations.indexOf(conv);
                                if (pos >= 0) {
                                    conversations.remove(pos);
                                    convAdapter.notifyItemRemoved(pos);
                                }
                                emptyState.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                            Toast.makeText(this, "Moved to " + target.getName(), Toast.LENGTH_SHORT).show();
                        }));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }));
    }

    private void showDeleteConversationDialog(ConversationEntity conv) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Delete \"" + conv.getTitle() + "\"?\nAll messages will be permanently removed.")
                .setPositiveButton("Delete", (d, w) ->
                        repo.deleteConversation(conv.getId(), unused -> runOnUiThread(() -> {
                            int pos = conversations.indexOf(conv);
                            if (pos >= 0) {
                                conversations.remove(pos);
                                convAdapter.notifyItemRemoved(pos);
                            }
                            emptyState.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
                            // Flag if the currently open conversation was deleted
                            if (conv.getId() == currentConvId) {
                                currentConvWasDeleted = true;
                            }
                            Toast.makeText(this, "Chat deleted.", Toast.LENGTH_SHORT).show();
                        })))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private EditText buildEditText(String initialText, String hint) {
        EditText et = new EditText(this);
        if (initialText != null) {
            et.setText(initialText);
            et.setSelection(initialText.length());
        }
        if (hint != null) {
            et.setHint(hint);
        }
        return et;
    }

    private View buildDialogView(EditText et) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginHorizontal = (int) (24 * getResources().getDisplayMetrics().density);
        int marginVertical = (int) (8 * getResources().getDisplayMetrics().density);
        lp.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
        et.setLayoutParams(lp);
        container.addView(et);
        return container;
    }
}
