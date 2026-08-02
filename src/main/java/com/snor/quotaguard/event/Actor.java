package com.snor.quotaguard.event;

import com.snor.quotaguard.domain.User;

import java.util.Optional;
import java.util.UUID;

public record Actor(UUID id, String email) {

    public static final Actor SYSTEM = new Actor(null, null);

    public static Actor of(User user) {
        return user == null ? SYSTEM : new Actor(user.getId(), user.getEmail());
    }

    public static Actor of(Optional<User> user) {
        return user.map(Actor::of).orElse(SYSTEM);
    }
}
