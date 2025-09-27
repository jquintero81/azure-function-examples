package org.example.core.port.out;

public interface NotificationPort {
    void send(String userId, String channel, String message);
}
