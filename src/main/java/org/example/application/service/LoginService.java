package org.example.application.service;


import org.example.core.model.MfaChallenge;
import org.example.core.port.out.MfaRepositoryPort;
import org.example.core.port.out.NotificationPort;
import org.example.core.port.out.UserRepositoryPort;

import java.util.Optional;

public class LoginService {
    private final UserRepositoryPort userRepo;
    private final MfaRepositoryPort mfaRepo;
    private final NotificationPort notification;

    public LoginService(UserRepositoryPort userRepo, MfaRepositoryPort mfaRepo, NotificationPort notification) {
        this.userRepo = userRepo;
        this.mfaRepo = mfaRepo;
        this.notification = notification;
    }

    public Optional<MfaChallenge> startLogin(String username) {
        return userRepo.findByUsername(username).map(user -> {
            String code = String.format("%06d", (int) (Math.random() * 1_000_000));
            MfaChallenge ch = mfaRepo.create(user.getId(), code);
            notification.send(user.getId(), user.getPreferredChannel(), "Your MFA code is: " + code);
            return ch;
        });
    }

    public boolean completeLogin(String username, String userId, String code) {
        // validate using repo which wraps Graph logic
        boolean ok = mfaRepo.validateAndConsume(userId, code);
        if (!ok) {
            int attempts = mfaRepo.incrementAttempts(userId);
            if (attempts >= 5) {
                mfaRepo.setHasMore(userId, false);
            }
            return false;
        }
        return true;
    }
}
