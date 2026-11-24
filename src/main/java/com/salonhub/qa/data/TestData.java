package com.salonhub.qa.data;

import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/** Random, readable test values. Every text starts with {@link TestSalon#PREFIX}. */
public final class TestData {

    private static final Faker FAKER = new Faker();

    private TestData() {
    }

    public static String name(String what) {
        return TestSalon.PREFIX + what + " " + FAKER.number().digits(6);
    }

    public static String sentence() {
        return TestSalon.PREFIX + FAKER.lorem().sentence(8);
    }

    /** A date 7 to 40 days ahead, so parallel tests seldom pick the same day. */
    public static LocalDate futureDate() {
        return LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(7, 41));
    }

    public static String jordanPhone() {
        return "079" + FAKER.number().digits(7);
    }

    public static String text(int length) {
        return "a".repeat(length);
    }
}
