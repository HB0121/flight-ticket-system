package com.example.flight.crawl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 爬虫执行服务，负责通过操作系统子进程方式启动 Docker 容器内的 Scrapy 爬虫。
 *
 * 执行流程：
 * 1. 拼接完整命令行（docker compose run --rm crawler scrapy crawl ...）
 * 2. 通过 ProcessBuilder 创建子进程
 * 3. 阻塞等待（带超时）爬虫完成
 * 4. 检查退出码和输出，失败时记录到 crawl_job 表
 *
 * 跨平台支持：通过 shellCommand() 方法根据操作系统选择不同的 Shell 包装器。
 * Windows 使用 cmd.exe /c，Linux/Mac 使用 sh -lc。
 *
 * 设计决策：
 *   - 使用子进程而非 HTTP 调用：爬虫运行在 Docker 中，通过 docker compose 启动
 *   - 同步等待（带超时）：爬虫通常 30 秒内完成，同步模型简单可靠
 *   - 输出截断：超长日志截取前 500 字符存入数据库，避免撑爆 error_message 字段
 */
@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);
    private final CrawlRepository crawlRepository;
    private final String command;       // 从配置文件中注入的爬虫启动命令前缀
    private final Duration timeout;     // 超时时间（默认 120 秒）

    /**
     * 构造器注入依赖和配置值。
     *
     * @param crawlRepository 爬虫任务数据仓库
     * @param command         命令前缀（来自 application.yml 的 app.crawler.command 配置）
     * @param timeoutSeconds  超时秒数（来自 app.crawler.timeout-seconds，默认 120）
     */
    public CrawlService(CrawlRepository crawlRepository,
                        @Value("${app.crawler.command}") String command,
                        @Value("${app.crawler.timeout-seconds:120}") long timeoutSeconds) {
        this.crawlRepository = crawlRepository;
        this.command = command;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    /**
     * 便捷方法：运行默认样本爬虫（无参数）。
     *
     * @return 最近一次爬虫任务记录
     */
    public CrawlJob runSampleCrawler() {
        return runCrawler(new CrawlRequest(null, null, null, null, null, null));
    }

    /**
     * 执行爬虫子进程的主方法。
     *
     * 完整执行逻辑：
     * 1. 拼接命令行：command + " " + 空格分隔的参数列表
     * 2. 构建 ProcessBuilder：工作目录设为 user.dir（确保能找到 docker-compose.yml）
     * 3. 启动子进程，同步等待（最大 timeout 秒）
     * 4. 读取子进程输出流（合并了 stdout 和 stderr，因为 redirectErrorStream(true)）
     * 5. 判断结果：
     *    - 超时 → 强制销毁进程，记录失败
     *    - 退出码非零 → 记录失败及输出摘要
     *    - 退出码为零 → 记录成功日志
     * 6. 若抛出异常（进程启动失败等），捕获并记录失败
     * 7. 返回最新的 crawl_job 记录（爬虫内部通过 Pipeline 已写入成功/失败状态）
     *
     * @param request 爬虫请求参数
     * @return 最近一次爬虫任务记录（异常时可能为兜底的空对象）
     */
    public CrawlJob runCrawler(CrawlRequest request) {
        // 拼接完整可执行命令
        String executableCommand = command + " " + String.join(" ", request.toCrawlerArguments());
        log.info("开始执行爬虫任务: {}", executableCommand);
        try {
            // 通过 ProcessBuilder 启动子进程
            Process process = new ProcessBuilder(shellCommand(executableCommand))
                    .directory(new File(System.getProperty("user.dir"))) // 工作目录设为当前项目根目录
                    .redirectErrorStream(true) // stderr 合并到 stdout，统一读取
                    .start();
            // 阻塞等待进程结束，最多等待 timeout 秒
            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            // 一次性读取全部输出（字节 → UTF-8 字符串）
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!completed) {
                // 超时：强制销毁子进程
                process.destroyForcibly();
                log.error("爬虫执行超时: {}", executableCommand);
                crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "爬虫执行超时：" + executableCommand);
            } else if (process.exitValue() != 0) {
                // 非零退出码：爬虫执行出错
                log.error("爬虫执行失败: exitCode={}, output={}", process.exitValue(), trimOutput(output));
                crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "爬虫执行失败：" + trimOutput(output));
            } else {
                // 正常完成
                log.info("爬虫执行成功: source={}", request.normalizedSource());
            }
        } catch (Exception ex) {
            // 进程启动失败或 I/O 异常
            log.error("爬虫启动失败: {}", ex.getMessage(), ex);
            crawlRepository.insertFailure(request.normalizedSource(), request.toSummary(), "爬虫启动失败：" + ex.getMessage());
        }

        // 返回最新记录：爬虫 Pipeline 已写入成功记录，此处也可读取刚写入的失败记录
        return crawlRepository.findLatest()
                .orElseGet(() -> new CrawlJob(null, "UNKNOWN", null, null, 0, 0, "暂无采集记录", request.normalizedSource(), request.toSummary()));
    }

    /**
     * 根据操作系统构建 Shell 包装命令。
     * Windows 上使用 cmd.exe /c 包装以保证 Docker Compose 命令正常执行；
     * 其他系统使用 sh -lc 包装。
     *
     * @param executableCommand 原始可执行命令字符串
     * @return 平台适配的命令数组，供 ProcessBuilder 使用
     */
    private String[] shellCommand(String executableCommand) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return new String[]{"cmd.exe", "/c", executableCommand}; // Windows: cmd /c "docker compose ..."
        }
        return new String[]{"sh", "-lc", executableCommand}; // Linux/Mac: sh -lc "docker compose ..."
    }

    /**
     * 截断输出文本，防止超长日志存入数据库。
     * 超过 500 字符时截取前 500 字符，避免撑爆 error_message 字段。
     *
     * @param output 原始进程输出
     * @return 截断后的输出文本
     */
    private String trimOutput(String output) {
        if (output == null || output.isBlank()) {
            return "无输出";
        }
        return output.length() > 500 ? output.substring(0, 500) : output;
    }
}
