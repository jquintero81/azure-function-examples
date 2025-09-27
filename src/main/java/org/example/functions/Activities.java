package org.example.functions;

import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import org.example.core.port.out.MfaRepositoryPort;
import org.example.core.port.out.UserRepositoryPort;
import org.example.di.SimpleContainer;

public class Activities {

    @FunctionName("CompensateInvalidateMfa")
    public Void compensateInvalidateMfa(@DurableActivityTrigger(name = "userId") String userId) {
        MfaRepositoryPort mfaRepo = SimpleContainer.get(MfaRepositoryPort.class);
        mfaRepo.invalidate(userId);
        return null;
    }

    @FunctionName("OnSuccessfulLogin")
    public Void onSuccessfulLogin(@DurableActivityTrigger(name = "username") String username) {
        UserRepositoryPort userRepo = SimpleContainer.get(UserRepositoryPort.class);
        // Optionally update lastLogin via Graph in a real adapter
        System.out.println("[Activity] successful login for " + username);
        return null;
    }
}
