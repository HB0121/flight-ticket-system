package com.example.flight.crawl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);
    private final CrawlRepository crawlRepository;
    private final String command;
    private final Duration timeout;
    private final String aerodataboxEnvKey;
    private final String aerodataboxConfigKey;

    public CrawlService(CrawlRepository crawlRepository,
                        @Value("${app.crawler.command}") String command,
                        @Value("${app.crawler.timeout-seconds:120}") long timeoutSeconds,
                        @Value("${AERODATABOX_KEY:}") String aerodataboxEnvKey,
                        @Value("${aerodatabox.key:}") String aerodataboxConfigKey) {
        this.crawlRepository = crawlRepository;
        this.command = command;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.aerodataboxEnvKey = aerodataboxEnvKey;
        this.aerodataboxConfigKey = aerodataboxConfigKey;
    }

    public CrawlJob runCrawler(CrawlRequest request) {
        List<String> commandArguments = buildCommandArguments(request);
        String executableCommand = String.join(" ", commandArguments);
        log.info("Starting crawler job: {}", executableCommand);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandArguments)
                    .directory(new File(System.getProperty("user.dir")))
                    .redirectErrorStream(true);
            applyProcessEnvironment(processBuilder.environment());

            Process process = processBuilder.start();
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readProcessOutput(process));
            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                String output = readCollectedOutput(outputFuture);
                log.error("Crawler timed out: {}", executableCommand);
                crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "Crawler timed out: " + executableCommand + "\n" + trimOutput(output));
            } else if (process.exitValue() != 0) {
                String output = readCollectedOutput(outputFuture);
                log.error("Crawler failed: exitCode={}, output={}", process.exitValue(), trimOutput(output));
                crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "Crawler failed: " + trimOutput(output));
            } else {
                readCollectedOutput(outputFuture);
                log.info("Crawler finished successfully: source={}", request.normalizedSource());
            }
        } catch (Exception ex) {
            log.error("Crawler startup failed: {}", ex.getMessage(), ex);
            crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "Crawler startup failed: " + ex.getMessage());
        }

        return crawlRepository.findLatest()
                .orElseGet(() -> new CrawlJob(null, "UNKNOWN", null, null, 0, 0, "No crawl history yet", request.normalizedSource(), request.toSummary()));
    }

    List<String> buildCommandArguments(CrawlRequest request) {
        var args = new ArrayList<>(splitCommand(command));
        args.addAll(request.toCrawlerArguments());
        return args;
    }

    void applyProcessEnvironment(Map<String, String> environment) {
        if (!hasText(environment.get("AERODATABOX_KEY")) && hasText(effectiveAerodataboxKey())) {
            environment.put("AERODATABOX_KEY", effectiveAerodataboxKey());
        }
    }

    String effectiveAerodataboxKey() {
        return hasText(aerodataboxEnvKey) ? aerodataboxEnvKey : aerodataboxConfigKey;
    }

    private List<String> splitCommand(String rawCommand) {
        var result = new ArrayList<String>();
        if (rawCommand == null || rawCommand.isBlank()) {
            return result;
        }
        var current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;
        for (int index = 0; index < rawCommand.length(); index++) {
            char ch = rawCommand.charAt(index);
            if ((ch == '"' || ch == '\'') && (!inQuote || ch == quoteChar)) {
                inQuote = !inQuote;
                quoteChar = inQuote ? ch : 0;
            } else if (Character.isWhitespace(ch) && !inQuote) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private String readProcessOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "Failed to read crawler output: " + ex.getMessage();
        }
    }

    private String readCollectedOutput(CompletableFuture<String> outputFuture) {
        try {
            return outputFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            return "Timed out while collecting crawler output: " + ex.getMessage();
        }
    }

    private String trimOutput(String output) {
        if (output == null || output.isBlank()) {
            return "no output";
        }
        return output.length() > 500 ? output.substring(0, 500) : output;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
