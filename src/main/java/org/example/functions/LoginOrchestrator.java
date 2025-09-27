package org.example.functions;

import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import org.example.application.service.LoginService;
import org.example.di.SimpleContainer;

import java.util.Map;

public class LoginOrchestrator {

    @FunctionName("LoginWithMfaOrchestrator")
    public String run(@DurableOrchestrationTrigger(name = "ctx") TaskOrchestrationContext ctx) throws Exception {
        Map<String, String> input = ctx.getInput(Map.class);
        String username = input.get("username");

        LoginService loginService = SimpleContainer.get(LoginService.class);

        String userId = null;
        String mfaId = null;
        boolean mfaSent = false;

        try {
            var maybeChallenge = loginService.startLogin(username);
            if (!maybeChallenge.isPresent()) return "INVALID_CREDENTIALS";
            var ch = maybeChallenge.get();
            userId = ch.getUserId();
            mfaId = ch.getId();
            mfaSent = true;

            String code = ctx.waitForExternalEvent("MfaCode", String.class).await();

            boolean ok = loginService.completeLogin(username, userId, code);
            if (!ok) {
                // compensation via activity
                ctx.callActivity("CompensateInvalidateMfa", userId).await();
                return "MFA_INVALID";
            }

            ctx.callActivity("OnSuccessfulLogin", username).await();
            return "OK";
        } catch (Exception ex) {
            if (mfaSent && userId != null) {
                ctx.callActivity("CompensateInvalidateMfa", userId).await();
            }
            throw ex;
        }
    }
}

