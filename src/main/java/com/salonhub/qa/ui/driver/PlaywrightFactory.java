package com.salonhub.qa.ui.driver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.salonhub.qa.config.Config;

import java.nio.file.Path;

/**
 * Owns one Playwright, Browser, BrowserContext and Page per test thread, so
 * TestNG can run UI tests in parallel. Each test gets a fresh context
 * (clean cookies and storage); the browser is shared by the thread.
 */
public final class PlaywrightFactory {

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

    private PlaywrightFactory() {
    }

    /** Opens a new context and page. Starts the browser on the first call of this thread. */
    public static Page start() {
        if (BROWSER.get() == null) {
            Playwright playwright = Playwright.create();
            PLAYWRIGHT.set(playwright);
            BROWSER.set(launch(playwright));
        }
        BrowserContext context = BROWSER.get().newContext(new Browser.NewContextOptions()
                .setBaseURL(Config.uiBaseUrl())
                .setViewportSize(Config.getInt("ui.viewport.width"), Config.getInt("ui.viewport.height"))
                .setLocale("en-US"));
        context.setDefaultTimeout(Config.getInt("ui.timeoutMs"));
        PlaywrightAssertions.setDefaultAssertionTimeout(Config.getInt("ui.timeoutMs"));
        context.setDefaultNavigationTimeout(Config.getInt("ui.navigationTimeoutMs"));
        if (Config.getBoolean("ui.trace")) {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(false));
        }
        CONTEXT.set(context);
        Page page = context.newPage();
        PAGE.set(page);
        return page;
    }

    public static Page page() {
        Page page = PAGE.get();
        if (page == null) {
            throw new IllegalStateException("No page for this thread. Call PlaywrightFactory.start() first.");
        }
        return page;
    }

    public static BrowserContext context() {
        return CONTEXT.get();
    }

    /**
     * Closes the context. When {@code tracePath} is not null and tracing is on,
     * the trace is saved there (open it with {@code mvn exec:java} or trace.playwright.dev).
     */
    public static void stop(Path tracePath) {
        BrowserContext context = CONTEXT.get();
        if (context != null) {
            if (Config.getBoolean("ui.trace")) {
                Tracing.StopOptions options = new Tracing.StopOptions();
                if (tracePath != null) {
                    options.setPath(tracePath);
                }
                context.tracing().stop(options);
            }
            context.close();
        }
        CONTEXT.remove();
        PAGE.remove();
    }

    /** Closes the browser of this thread. Call once when the thread is done. */
    public static void quit() {
        if (BROWSER.get() != null) {
            BROWSER.get().close();
        }
        if (PLAYWRIGHT.get() != null) {
            PLAYWRIGHT.get().close();
        }
        BROWSER.remove();
        PLAYWRIGHT.remove();
    }

    private static Browser launch(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(Config.getBoolean("ui.headless"))
                .setSlowMo(Config.getInt("ui.slowMoMs"));
        String browser = Config.get("ui.browser").toLowerCase();
        return switch (browser) {
            case "chromium" -> playwright.chromium().launch(options);
            case "chrome" -> playwright.chromium().launch(options.setChannel("chrome"));
            case "edge" -> playwright.chromium().launch(options.setChannel("msedge"));
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit" -> playwright.webkit().launch(options);
            default -> throw new IllegalArgumentException("Unknown ui.browser: " + browser
                    + " (use chromium, chrome, edge, firefox or webkit)");
        };
    }
}
