package com.example.flight.crawl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class CrawlService {
    private final CrawlRepository crawlRepository;
    private final String command;
    private final Duration timeout;

    public CrawlService(CrawlRepository crawlRepository,
                        @Value("${app.crawler.command}") String command,
                        @Value("${app.crawler.timeout-seconds:120}") long timeoutSeconds) {
        this.crawlRepository = crawlRepository;
        this.command = command;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public CrawlJob runSampleCrawler() {
        return runCrawler(new CrawlRequest(null, null, null, null, null, null));
    }

    public CrawlJob runCrawler(CrawlRequest request) {
        String executableCommand = command + " " + String.join(" ", request.toCrawlerArguments());
        try {
            Process process = new ProcessBuilder(shellCommand(executableCommand))
                    .directory(new File(System.getProperty("user.dir")))
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!completed) {
                process.destroyForcibly();
                crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "爬虫执行超时：" + executableCommand);
            } else if (process.exitValue() != 0) {
                crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "爬虫执行失败：" + trimOutput(output));
            }
        } catch (Exception ex) {
            crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "爬虫启动失败：" + ex.getMessage());
        }

        return crawlRepository.findLatest()
                .orElseGet(() -> new CrawlJob(null, "UNKNOWN", null, null, 0, 0, "暂无采集记录", request.normalizedSource(), request.toSummary()));
    }

    private String[] shellCommand(String executableCommand) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return new String[]{"cmd.exe", "/c", executableCommand};
        }
        return new String[]{"sh", "-lc", executableCommand};
    }

    private String trimOutput(String output) {
        if (output == null || output.isBlank()) {
            return "无输出";
        }
        return output.length() > 500 ? output.substring(0, 500) : output;
    }
}
