package com.jo.agrisenseai;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ChatAdapter — shows user/AI/checklist messages.
 *
 * Multi-select mode (ChatGPT-style):
 *  • Long press any item → enters selection mode, selects that item.
 *  • Tap in selection mode → toggles selection on that item.
 *  • Selected items get a subtle green background highlight.
 *  • Host (AIAssistantFragment) is notified via {@link SelectionListener}.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Selection highlight: 15% opacity green
    private static final int COLOR_SELECTED   = Color.argb(38, 56, 142, 60);   // #26388E3C
    private static final int COLOR_DESELECTED = Color.TRANSPARENT;

    private final List<ChatMessage> messageList;

    // ── Selection state ────────────────────────────────────────────────────────
    private boolean isSelectionMode = false;

    /** Timestamps of messages currently marked as selected. */
    private final Set<Long> selectedTimestamps = new HashSet<>();

    // ── Listener ───────────────────────────────────────────────────────────────
    public interface SelectionListener {
        /** Called when selection mode is turned on (true) or off (false). */
        void onSelectionModeChanged(boolean active);
        /** Called whenever the selected-item count changes. */
        void onSelectionCountChanged(int count);
    }

    private SelectionListener selectionListener;

    // ── Constructor ────────────────────────────────────────────────────────────
    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public int getSelectedCount() {
        return selectedTimestamps.size();
    }

    /** Returns a copy of the selected timestamps (safe to iterate/mutate). */
    public Set<Long> getSelectedTimestamps() {
        return new HashSet<>(selectedTimestamps);
    }

    /**
     * Selects every non-loading message in the list.
     * Must be called on the main thread.
     */
    public void selectAll() {
        for (ChatMessage msg : messageList) {
            if (!msg.isLoading()) {
                selectedTimestamps.add(msg.getTimestamp());
            }
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionCountChanged(selectedTimestamps.size());
        }
    }

    /**
     * Exits selection mode and clears all selections.
     * Must be called on the main thread.
     */
    public void exitSelectionMode() {
        isSelectionMode = false;
        selectedTimestamps.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionModeChanged(false);
        }
    }

    // ── RecyclerView.Adapter ───────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ChatMessage.TYPE_USER) {
            return new UserMessageViewHolder(
                    inflater.inflate(R.layout.item_chat_bubble_user, parent, false));
        } else if (viewType == ChatMessage.TYPE_CHECKLIST) {
            return new ChecklistViewHolder(
                    inflater.inflate(R.layout.item_chat_checklist, parent, false));
        } else {
            return new AIMessageViewHolder(
                    inflater.inflate(R.layout.item_chat_bubble_ai, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        boolean selected = selectedTimestamps.contains(message.getTimestamp());

        // ── Selection highlight ──
        holder.itemView.setBackgroundColor(
                (isSelectionMode && selected) ? COLOR_SELECTED : COLOR_DESELECTED);

        // ── Long press → enter selection mode ──
        holder.itemView.setOnLongClickListener(v -> {
            if (message.isLoading()) return false; // don't select loading bubbles
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return false;

            if (!isSelectionMode) {
                isSelectionMode = true;
                if (selectionListener != null) selectionListener.onSelectionModeChanged(true);
            }
            toggleSelection(pos);
            return true;
        });

        // ── Tap → toggle selection when in selection mode ──
        holder.itemView.setOnClickListener(v -> {
            if (!isSelectionMode) return;
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && !message.isLoading()) {
                toggleSelection(pos);
            }
        });

        // ── Bind content ──
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AIMessageViewHolder) {
            ((AIMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ChecklistViewHolder) {
            ((ChecklistViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void toggleSelection(int position) {
        ChatMessage msg = messageList.get(position);
        long ts = msg.getTimestamp();
        if (selectedTimestamps.contains(ts)) {
            selectedTimestamps.remove(ts);
        } else {
            selectedTimestamps.add(ts);
        }
        notifyItemChanged(position);
        if (selectionListener != null) {
            selectionListener.onSelectionCountChanged(selectedTimestamps.size());
        }
    }

    // ── View Holders ───────────────────────────────────────────────────────────

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.chatMessageText);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());
        }
    }

    static class AIMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final ProgressBar loadingProgress;

        AIMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.chatMessageText);
            loadingProgress = itemView.findViewById(R.id.chatMessageLoading);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());
            loadingProgress.setVisibility(message.isLoading() ? View.VISIBLE : View.GONE);
        }
    }

    static class ChecklistViewHolder extends RecyclerView.ViewHolder {
        private final TextView item1;
        private final TextView item2;
        private final TextView item3;

        ChecklistViewHolder(@NonNull View itemView) {
            super(itemView);
            item1 = itemView.findViewById(R.id.checklistItem1);
            item2 = itemView.findViewById(R.id.checklistItem2);
            item3 = itemView.findViewById(R.id.checklistItem3);
        }

        void bind(ChatMessage message) {
            List<String> items = message.getChecklistItems();
            if (items != null && items.size() >= 3) {
                item1.setText(items.get(0));
                item2.setText(items.get(1));
                item3.setText(items.get(2));
            } else {
                item1.setText("");
                item2.setText("");
                item3.setText("");
            }
        }
    }
}
