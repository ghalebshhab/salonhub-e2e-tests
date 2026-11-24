package com.salonhub.qa.ui.driver;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.client.SessionCache;
import com.salonhub.qa.config.Config;
import com.salonhub.qa.data.TestAccount;
import io.qameta.allure.Step;

import java.net.URI;
import java.util.List;

/**
 * Signs a browser context in without the login form: it copies the session
 * cookies of a cached API sign-in into the browser. Use the login form only
 * in tests of the login page itself.
 */
public final class BrowserSession {

    private BrowserSession() {
    }

    @Step("Sign the browser in as {account} (API shortcut)")
    public static void signIn(BrowserContext context, TestAccount account) {
        ApiSession session = SessionCache.get(account);
        String host = URI.create(Config.uiBaseUrl()).getHost();
        List<Cookie> cookies = session.cookies().asList().stream()
                .filter(c -> c.getValue() != null && !c.getValue().isEmpty())
                .map(c -> new Cookie(c.getName(), c.getValue())
                        .setDomain(host)
                        .setPath(c.hasPath() ? c.getPath() : "/")
                        .setHttpOnly(c.isHttpOnly())
                        .setSecure(false)
                        .setSameSite(SameSiteAttribute.STRICT))
                .toList();
        if (cookies.isEmpty()) {
            throw new IllegalStateException("The API session of " + account + " has no cookies");
        }
        context.addCookies(cookies);
    }
}
