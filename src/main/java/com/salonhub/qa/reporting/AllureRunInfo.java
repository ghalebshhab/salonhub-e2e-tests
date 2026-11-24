package com.salonhub.qa.reporting;

import com.salonhub.qa.config.Config;
import org.testng.IExecutionListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Before the run, writes the files that make the Allure report useful:
 * <ul>
 *   <li>{@code environment.properties} - URLs, browser, suite, Java and OS</li>
 *   <li>{@code categories.json} - groups failures (product bug, timeout, server down, ...)</li>
 *   <li>{@code executor.json} - the CI build name and link (GitHub Actions only)</li>
 * </ul>
 * TestNG loads this listener through {@code META-INF/services/org.testng.ITestNGListener}.
 */
public class AllureRunInfo implements IExecutionListener {

    @Override
    public void onExecutionStart() {
        Path results = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));
        try {
            Files.createDirectories(results);
            writeEnvironment(results);
            copyCategories(results);
            writeExecutor(results);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the Allure run info", e);
        }
    }

    private static void writeEnvironment(Path results) throws IOException {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("Suite", System.getProperty("suite", "regression"));
        env.put("UI URL", Config.uiBaseUrl());
        env.put("API URL", Config.apiBaseUrl());
        env.put("Browser", Config.get("ui.browser") + (Config.getBoolean("ui.headless") ? " (headless)" : ""));
        env.put("Viewport", Config.get("ui.viewport.width") + "x" + Config.get("ui.viewport.height"));
        env.put("Test salon", Config.get("data.salon.id"));
        env.put("Java", System.getProperty("java.version"));
        env.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        env.put("Run by", System.getenv("GITHUB_ACTIONS") != null ? "GitHub Actions" : "local machine");

        StringBuilder text = new StringBuilder();
        env.forEach((key, value) -> text.append(key.replace(" ", "\\ ")).append('=').append(value).append('\n'));
        Files.writeString(results.resolve("environment.properties"), text, StandardCharsets.UTF_8);
    }

    private static void copyCategories(Path results) throws IOException {
        try (InputStream in = AllureRunInfo.class.getResourceAsStream("/allure/categories.json")) {
            if (in != null) {
                Files.copy(in, results.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void writeExecutor(Path results) throws IOException {
        String runId = System.getenv("GITHUB_RUN_ID");
        if (runId == null) {
            return;
        }
        String buildUrl = System.getenv("GITHUB_SERVER_URL") + "/" + System.getenv("GITHUB_REPOSITORY")
                + "/actions/runs/" + runId;
        String json = """
                {
                  "name": "GitHub Actions",
                  "type": "github",
                  "buildName": "%s #%s",
                  "buildOrder": %s,
                  "buildUrl": "%s",
                  "reportName": "SalonHub E2E"
                }
                """.formatted(System.getenv("GITHUB_WORKFLOW"), System.getenv("GITHUB_RUN_NUMBER"),
                System.getenv("GITHUB_RUN_NUMBER"), buildUrl);
        Files.writeString(results.resolve("executor.json"), json, StandardCharsets.UTF_8);
    }
}
