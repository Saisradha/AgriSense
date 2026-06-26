package com.jo.agrisenseai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationListAdapter extends RecyclerView.Adapter<ConversationListAdapter.ViewHolder> {

    public interface ConversationListener {
        void onConversationClick(ConversationEntity conversation);
        void onConversationLongClick(View anchorView, ConversationEntity conversation);
    }

    private final List<ConversationEntity> conversations;
    private final ConversationListener     listener;

    public ConversationListAdapter(List<ConversationEntity> conversations, ConversationListener listener) {
        this.conversations = conversations;
        this.listener      = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationEntity conv = conversations.get(position);

        holder.title.setText(conv.getTitle() != null && !conv.getTitle().isEmpty()
                ? conv.getTitle() : "New Chat");

        String last = conv.getLastMessage();
        if (last != null && !last.isEmpty()) {
            holder.lastMessage.setText(last);
            holder.lastMessage.setVisibility(View.VISIBLE);
        } else {
            holder.lastMessage.setVisibility(View.GONE);
        }

        holder.date.setText(formatDate(conv.getCreatedAt()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onConversationClick(conv);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onConversationLongClick(v, conv);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    // ── Date formatting ────────────────────────────────────────────────────────

    private String formatDate(long timestamp) {
        Calendar now  = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(timestamp);

        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        } else if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(timestamp));
        } else {
            return new SimpleDateFormat("MMM d, yy", Locale.getDefault()).format(new Date(timestamp));
        }
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView lastMessage;
        final TextView date;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title       = itemView.findViewById(R.id.conversationTitle);
            lastMessage = itemView.findViewById(R.id.conversationLastMessage);
            date        = itemView.findViewById(R.id.conversationDate);
        }
    }
}
