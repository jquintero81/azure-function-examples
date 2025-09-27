package org.example.core.model;

public class User {
    private final String id;
    private final String username;
    private final String preferredChannel;

    public User(String id, String username, String preferredChannel) {
        this.id = id;
        this.username = username;
        this.preferredChannel = preferredChannel;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPreferredChannel() {
        return preferredChannel;
    }
}