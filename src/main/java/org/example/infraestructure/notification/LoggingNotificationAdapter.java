package org.example.infraestructure.notification;

import org.example.core.port.out.NotificationPort;

public class LoggingNotificationAdapter implements NotificationPort {
    @Override
    public void send(String userId, String channel, String message) {
        System.out.println("[Notification] user=" + userId + " channel=" + channel + " msg=" + message);
    }
}
