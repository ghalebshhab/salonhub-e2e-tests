package com.salonhub.qa.api.client;

import com.salonhub.qa.api.endpoints.AuthApi;
import com.salonhub.qa.data.TestAccount;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Signs each account in once per test run and shares the session.
 * The backend allows only 10 login calls per 5 minutes per IP
 * ({@code AuthRateLimitFilter}), so tests must not sign in again and again.
 * <p>
 * Tests that end or change a session (logout, password change) must use
 * {@link AuthApi#signIn(TestAccount)} directly, never this cache.
 */
public final class SessionCache {

    private static final Map<String, ApiSession> SESSIONS = new ConcurrentHashMap<>();
    private static final AuthApi AUTH_API = new AuthApi();

    private SessionCache() {
    }

    public static ApiSession get(TestAccount account) {
        return SESSIONS.computeIfAbsent(account.email(), email -> AUTH_API.signIn(account));
    }

    /** Forgets a session, for example after a test made it invalid. */
    public static void evict(TestAccount account) {
        SESSIONS.remove(account.email());
    }
}
