package org.example.core.model;

public class MfaChallenge {
    private final String id;
    private final String userId;
    private final String code;

    public MfaChallenge(String id, String userId, String code) {
        this.id = id;
        this.userId = userId;
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCode() {
        return code;
    }
}
