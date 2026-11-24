package com.salonhub.qa.utils;

import java.time.Duration;

/** Retries a check until it passes or the time is up. Use it for API state that the UI changes. */
public final class Eventually {

    private Eventually() {
    }

    public static void check(Runnable assertion) {
        check(Duration.ofSeconds(10), assertion);
    }

    public static void check(Duration timeout, Runnable assertion) {
        long end = System.nanoTime() + timeout.toNanos();
        while (true) {
            try {
                assertion.run();
                return;
            } catch (AssertionError | RuntimeException e) {
                if (System.nanoTime() > end) {
                    throw e;
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
