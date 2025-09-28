package org.example.application.service;

import org.example.core.model.MfaChallenge;
import org.example.core.port.out.MfaRepositoryPort;
import org.example.core.port.out.NotificationPort;
import org.example.core.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTest {

    private UserRepositoryPort userRepo;
    private MfaRepositoryPort mfaRepo;
    private NotificationPort notification;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepositoryPort.class);
        mfaRepo = mock(MfaRepositoryPort.class);
        notification = mock(NotificationPort.class);
        loginService = new LoginService(userRepo, mfaRepo, notification);
    }

    @Test
    void startLogin_userFound_sendsNotificationAndReturnsChallenge() {
        // given
        var user = mock(org.example.core.model.User.class);
        when(userRepo.findByUsername("user")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn("id1");
        when(user.getPreferredChannel()).thenReturn("email");
        var challenge = mock(MfaChallenge.class);
        when(mfaRepo.create(eq("id1"), anyString())).thenReturn(challenge);

        // when
        Optional<MfaChallenge> result = loginService.startLogin("user");

        // then
        assertTrue(result.isPresent());
        verify(notification).send(eq("id1"), eq("email"), contains("Your MFA code is:"));
    }

    @Test
    void startLogin_userNotFound_returnsEmpty() {
        // given
        when(userRepo.findByUsername("nouser")).thenReturn(Optional.empty());

        // when
        Optional<MfaChallenge> result = loginService.startLogin("nouser");

        // then
        assertFalse(result.isPresent());
        verifyNoMoreInteractions(notification, mfaRepo);
    }

    @Test
    void completeLogin_validCode_returnsTrue() {
        // given
        when(mfaRepo.validateAndConsume("id1", "123456")).thenReturn(true);

        // when
        boolean result = loginService.completeLogin("user", "id1", "123456");

        // then
        assertTrue(result);
        verify(mfaRepo).validateAndConsume("id1", "123456");
        verifyNoMoreInteractions(mfaRepo);
    }

    @Test
    void completeLogin_invalidCode_lessThanFiveAttempts_returnsFalse() {
        // given
        when(mfaRepo.validateAndConsume("id1", "badcode")).thenReturn(false);
        when(mfaRepo.incrementAttempts("id1")).thenReturn(3);

        // when
        boolean result = loginService.completeLogin("user", "id1", "badcode");

        // then
        assertFalse(result);
        verify(mfaRepo).validateAndConsume("id1", "badcode");
        verify(mfaRepo).incrementAttempts("id1");
        verify(mfaRepo, never()).setHasMore(anyString(), anyBoolean());
    }

    @Test
    void completeLogin_invalidCode_fiveAttempts_setsHasMoreFalse_returnsFalse() {
        // given
        when(mfaRepo.validateAndConsume("id1", "badcode")).thenReturn(false);
        when(mfaRepo.incrementAttempts("id1")).thenReturn(5);

        // when
        boolean result = loginService.completeLogin("user", "id1", "badcode");

        // then
        assertFalse(result);
        verify(mfaRepo).validateAndConsume("id1", "badcode");
        verify(mfaRepo).incrementAttempts("id1");
        verify(mfaRepo).setHasMore("id1", false);
    }
}

