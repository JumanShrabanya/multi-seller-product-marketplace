package com.js.multi_seller.service;

import com.js.multi_seller.entity.UserRole;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login sessions. A logged-in account is handed an opaque token that the
 * seller endpoints exchange for the account id, so a client cannot act as another
 * account by simply changing a header. Sessions live in memory only: they are lost on
 * restart and are per-instance, which is acceptable for this demo.
 */
@Component
public class SessionStore {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session create(long userId, UserRole role) {
        Session session = new Session(UUID.randomUUID().toString(), userId, role);
        sessions.put(session.token(), session);
        return session;
    }

    public Optional<Session> find(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    public record Session(String token, long userId, UserRole role) {
    }
}
