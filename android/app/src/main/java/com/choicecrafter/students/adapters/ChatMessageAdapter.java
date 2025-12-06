package com.choicecrafter.studentapp.adapters;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.ChatMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        Log.i("ChatMessageAdapter", "getItemViewType called for message: " + message.getText() + " from " + message.getSenderId() + " currentUserId: " + currentUserId);
        if (currentUserId != null && currentUserId.equals(message.getSenderId())) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_TYPE_SENT ? R.layout.item_chat_message_sent : R.layout.item_chat_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            holder.senderText.setText("You");
        } else {
            holder.senderText.setText(message.getSenderId());
        }
        holder.messageText.setText(message.getText());
        holder.timeText.setText(DateFormat.format("HH:mm", new Date(message.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderText;
        TextView messageText;
        TextView timeText;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.chatMessageSender);
            messageText = itemView.findViewById(R.id.chatMessageText);
            timeText = itemView.findViewById(R.id.chatMessageTime);
        }
    }
}
