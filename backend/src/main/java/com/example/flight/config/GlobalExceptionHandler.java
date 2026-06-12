package com.example.flight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器，使用 Spring AOP 机制拦截所有 Controller 抛出的异常。
 *
 * 设计模式：面向切面编程（AOP）—— @ControllerAdvice 将此类织入所有 @RestController，
 * 使得业务代码无需在 Controller 层重复编写 try-catch 块。
 *
 * 每个 @ExceptionHandler 方法对应一种异常类型，返回统一的 ErrorResponse JSON 体。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 JSR-303 Bean Validation 校验失败的异常。
     * 收集所有字段级错误信息，拼接后返回 400 响应。
     *
     * @param ex 携带字段校验失败明细的异常对象
     * @return 400 Bad Request，body 中包含各字段的校验失败描述
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // 从 BindingResult 中提取所有字段错误，格式化为 "字段名: 错误信息" 的拼接字符串
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("请求参数校验失败");
        log.warn("请求参数校验失败: {}", errors);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "参数校验失败", errors));
    }

    /**
     * 处理非法参数异常（通常由业务层手动抛出）。
     *
     * @param ex 携带错误描述信息的 IllegalArgumentException
     * @return 400 Bad Request，body 中包含参数错误说明
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "请求参数错误", ex.getMessage()));
    }

    /**
     * 处理数据库访问异常（连接失败、查询超时等）。
     * 返回 503 Service Unavailable，提示用户数据库暂不可用。
     *
     * @param ex Spring 封装的 DataAccessException 或其子类
     * @return 503 Service Unavailable
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException ex) {
        log.error("数据库访问异常", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, "数据库服务不可用", "数据库连接或查询失败，请稍后重试"));
    }

    /**
     * 处理资源未找到异常（Spring MVC 6 中替代了旧的 404 处理方式）。
     *
     * @param ex NoResourceFoundException
     * @return 404 Not Found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "资源不存在", ex.getMessage()));
    }

    /**
     * 兜底异常处理器：捕获所有未被上方特定处理器匹配的异常。
     * 返回 500 Internal Server Error，不暴露内部细节给客户端。
     *
     * @param ex 任意未预期的异常
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("未预期异常", ex); // 完整堆栈仅记录到服务端日志，不返回给客户端
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "服务器内部错误", "处理请求时发生未预期错误，请查看日志"));
    }
}
