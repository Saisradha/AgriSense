package com.jo.agrisenseai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SidebarExpandableListAdapter extends BaseExpandableListAdapter {

    private List<FolderEntity> folders = new ArrayList<>();
    private Map<Long, List<ConversationEntity>> conversationMap = new HashMap<>();
    private long currentConversationId = -1L;

    public void setData(List<FolderEntity> folders, Map<Long, List<ConversationEntity>> conversationMap) {
        this.folders = folders != null ? folders : new ArrayList<>();
        this.conversationMap = conversationMap != null ? conversationMap : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setCurrentConversationId(long id) {
        this.currentConversationId = id;
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return folders.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        FolderEntity folder = folders.get(groupPosition);
        List<ConversationEntity> list = conversationMap.get(folder.getId());
        return list != null ? list.size() : 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return folders.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        FolderEntity folder = folders.get(groupPosition);
        List<ConversationEntity> list = conversationMap.get(folder.getId());
        return list != null ? list.get(childPosition) : null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return folders.get(groupPosition).getId();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        ConversationEntity conv = (ConversationEntity) getChild(groupPosition, childPosition);
        return conv != null ? conv.getId() : -1L;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        FolderEntity folder = (FolderEntity) getGroup(groupPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sidebar_folder, parent, false);
        }

        TextView folderName = convertView.findViewById(R.id.folderName);
        ImageView indicator = convertView.findViewById(R.id.indicator);

        folderName.setText(folder.getName());
        
        // Update arrow indicator
        indicator.setRotation(isExpanded ? 90f : 0f);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ConversationEntity conv = (ConversationEntity) getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sidebar_conversation, parent, false);
        }

        TextView title = convertView.findViewById(R.id.conversationTitle);
        ImageView icon = convertView.findViewById(R.id.chatIcon);

        title.setText(conv.getTitle() != null && !conv.getTitle().isEmpty() ? conv.getTitle() : "New Chat");

        // Highlight if this is the currently active conversation
        if (conv.getId() == currentConversationId) {
            int color = parent.getContext().getResources().getColor(R.color.primary_green);
            title.setTextColor(color);
            icon.setColorFilter(color);
        } else {
            int textColor = parent.getContext().getResources().getColor(R.color.on_surface);
            int iconColor = parent.getContext().getResources().getColor(R.color.accent_blue);
            title.setTextColor(textColor);
            icon.setColorFilter(iconColor);
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
