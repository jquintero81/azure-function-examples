package org.example.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.durabletask.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;

import java.util.Map;
import java.util.Optional;

public class HttpTriggers {
    private final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("StartLoginOrchestration")
    public HttpResponseMessage start(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableClient") DurableTaskClient client,
            final ExecutionContext context) throws Exception {

        String body = request.getBody().orElseThrow(() -> new IllegalArgumentException("body required"));
        Map<String, String> payload = mapper.readValue(body, Map.class);
        String username = payload.get("username");

        Map<String, String> input = Map.of("username", username);
        String instanceId = client.scheduleNewOrchestrationInstance("LoginWithMfaOrchestrator", input);

        return request.createResponseBuilder(HttpStatus.OK)
                .body(mapper.writeValueAsString(Map.of("instanceId", instanceId)))
                .build();
    }

    @FunctionName("RaiseMfaCodeEvent")
    public HttpResponseMessage raiseEvent(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableClient") DurableTaskClient client,
            final ExecutionContext context) throws Exception {
        String body = request.getBody().orElseThrow(() -> new IllegalArgumentException("body required"));
        Map<String, String> payload = mapper.readValue(body, Map.class);
        String instanceId = payload.get("instanceId");
        String code = payload.get("code");

        client.raiseEvent(instanceId, "MfaCode", code);

        return request.createResponseBuilder(HttpStatus.OK).body("event raised").build();
    }

}

