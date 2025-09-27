package org.example.core.port.out;

import org.example.core.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findByUsername(String username);
}
