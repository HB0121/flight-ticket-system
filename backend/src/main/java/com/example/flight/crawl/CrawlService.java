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
        try {
            Process process = new ProcessBuilder(shellCommand())
                    .directory(new File(System.getProperty("user.dir")))
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!completed) {
                process.destroyForcibly();
                crawlRepository.insertFailure("爬虫执行超时：" + command);
            } else if (process.exitValue() != 0) {
                crawlRepository.insertFailure("爬虫执行失败：" + trimOutput(output));
            }
        } catch (Exception ex) {
            crawlRepository.insertFailure("爬虫启动失败：" + ex.getMessage());
        }

        return crawlRepository.findLatest()
                .orElseGet(() -> new CrawlJob(null, "UNKNOWN", null, null, 0, 0, "暂无采集记录"));
    }

    private String[] shellCommand() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return new String[]{"cmd.exe", "/c", command};
        }
        return new String[]{"sh", "-lc", command};
    }

    private String trimOutput(String output) {
        if (output == null || output.isBlank()) {
            return "无输出";
        }
        return output.length() > 500 ? output.substring(0, 500) : output;
    }
}

