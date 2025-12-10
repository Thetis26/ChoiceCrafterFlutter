package com.choicecrafter.students.models;

import java.util.List;

public class Conversation {
    private String id;
    private String title;
    private String lastMessage;
    private long timestamp;
    private boolean unread;
    private List<String> participants;
    private List<String> formerParticipants;
    private boolean readOnly;
    private List<String> unreadBy;
    private String lastMessageSenderId;

    public Conversation() {
    }

    public Conversation(String id, String title, String lastMessage, long timestamp, boolean unread, List<String> participants) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unread = unread;
        this.participants = participants;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public List<String> getFormerParticipants() {
        return formerParticipants;
    }

    public void setFormerParticipants(List<String> formerParticipants) {
        this.formerParticipants = formerParticipants;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public List<String> getUnreadBy() {
        return unreadBy;
    }

    public void setUnreadBy(List<String> unreadBy) {
        this.unreadBy = unreadBy;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }
}
