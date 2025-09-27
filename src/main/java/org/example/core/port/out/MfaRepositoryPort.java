package org.example.core.port.out;

import org.example.core.model.MfaChallenge;

public interface MfaRepositoryPort {
    MfaChallenge create(String userId, String code);

    boolean validateAndConsume(String userId, String code);

    void invalidate(String userId);

    int incrementAttempts(String userId);

    void setHasMore(String userId, boolean value);
}
