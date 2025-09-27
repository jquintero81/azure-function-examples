package org.example.infraestructure.graph;


import com.azure.identity.*;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.example.core.model.MfaChallenge;
import org.example.core.port.out.MfaRepositoryPort;
import org.example.core.port.out.UserRepositoryPort;

import java.util.*;

public class AzureAdB2cUserRepository implements UserRepositoryPort, MfaRepositoryPort {
    private final GraphServiceClient graphClient;
    private final String extPrefix; // extension_{appId}_

    public AzureAdB2cUserRepository() {
        String tenantId = System.getenv("GRAPH_TENANT_ID");
        String clientId = System.getenv("GRAPH_CLIENT_ID");
        String clientSecret = System.getenv("GRAPH_CLIENT_SECRET");
        String b2cExtAppId = System.getenv("B2C_EXT_APP_CLIENT_ID");

        DeviceCodeCredential deviceCodeCredential = new DeviceCodeCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .challengeConsumer(challenge -> System.out.println(challenge.getMessage()))
                .build();

        String[] scopes = new String[]{"https://graph.microsoft.com/.default"};

        this.graphClient = new GraphServiceClient(deviceCodeCredential, scopes);
        this.extPrefix = "extension_" + b2cExtAppId + "_";

    }


    // --- UserRepositoryPort ---
    @Override
    public Optional<org.example.core.model.User> findByUsername(String username) {
        try {
            List<User> users = graphClient.users().get(config -> {
                config.queryParameters.filter = "userPrincipalName eq '" + username + "'";
                config.queryParameters.select = new String[]{"id", "displayName", "userPrincipalName"};
            }).getValue();

            if (users == null || users.isEmpty()) return Optional.empty();

            User msUser = users.get(0);
            org.example.core.model.User domainUser = new org.example.core.model.User(
                    msUser.getId(),
                    msUser.getDisplayName(),
                    msUser.getUserPrincipalName()
            );
            return Optional.of(domainUser);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }


    // --- MfaRepositoryPort ---
    @Override
    public MfaChallenge create(String userId, String code) {
        String attrOtp = extPrefix + "otp";
        String attrAttempts = extPrefix + "otpAttempts";

        User userPatch = new User();
        userPatch.setAdditionalData(new HashMap<>());
        userPatch.getAdditionalData().put(attrOtp, code);
        userPatch.getAdditionalData().put(attrAttempts, 0);

        graphClient.users().byUserId(userId).patch(userPatch);

        String challengeId = UUID.randomUUID().toString();
        return new MfaChallenge(challengeId, userId, code);
    }

    @Override
    public boolean validateAndConsume(String userId, String code) {
        try {
            User user = graphClient.users().byUserId(userId)
                    .get(config -> {
                        config.queryParameters.select = new String[]{"id", extPrefix + "otp"};
                    });

            if (user == null) return false;
            Object stored = user.getAdditionalData().get(extPrefix + "otp");
            if (stored == null) return false;
            String storedStr = stored.toString();
            if (!storedStr.equals(code)) return false;

            // Consumir: set otp = null
            User patch = new User();
            patch.setAdditionalData(new HashMap<>());
            patch.getAdditionalData().put(extPrefix + "otp", null);
            graphClient.users().byUserId(userId).patch(patch);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void invalidate(String userId) {
        User userPatch = new User();
        userPatch.setAdditionalData(new HashMap<>());
        userPatch.getAdditionalData().put(extPrefix + "otp", null);
        graphClient.users().byUserId(userId).patch(userPatch);
    }

    @Override
    public int incrementAttempts(String userId) {
        User user = graphClient.users().byUserId(userId)
                .get(config -> {
                    config.queryParameters.select = new String[]{"id", extPrefix + "otpAttempts"};
                });

        int cur = 0;
        Object curObj = user.getAdditionalData().get(extPrefix + "otpAttempts");
        if (curObj != null) {
            try {
                cur = Integer.parseInt(curObj.toString());
            } catch (Exception e) {
                cur = 0;
            }
        }
        int next = cur + 1;
        User userPatch = new User();
        userPatch.setAdditionalData(new HashMap<>());
        userPatch.getAdditionalData().put(extPrefix + "otpAttempts", next);
        graphClient.users().byUserId(userId).patch(userPatch);
        return next;
    }

    @Override
    public void setHasMore(String userId, boolean value) {
        User userPatch = new User();
        userPatch.setAdditionalData(new HashMap<>());
        userPatch.getAdditionalData().put(extPrefix + "hasMore", value);
        graphClient.users().byUserId(userId).patch(userPatch);
    }
}

