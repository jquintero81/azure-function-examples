package org.example.di;

import com.google.inject.AbstractModule;
import org.example.core.port.out.MfaRepositoryPort;
import org.example.core.port.out.NotificationPort;
import org.example.core.port.out.UserRepositoryPort;
import org.example.infraestructure.graph.AzureAdB2cUserRepository;
import org.example.infraestructure.notification.LoggingNotificationAdapter;

public class FunctionsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(UserRepositoryPort.class).to(AzureAdB2cUserRepository.class);
        bind(MfaRepositoryPort.class).to(AzureAdB2cUserRepository.class);
        bind(NotificationPort.class).to(LoggingNotificationAdapter.class);
    }
}
