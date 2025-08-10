package com.myweb.website_core.common.exception.handle;

import com.myweb.website_core.common.exception.security.AccountLockedException;
import com.myweb.website_core.common.exception.security.AuthenticationException;
import com.myweb.website_core.common.exception.security.TokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<String> handleLockedException(LockedException e) {
        log.warn("账户锁定: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(LockedException e) {
        log.warn("账户认证错误: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(e.getMessage());
    }
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<String> handleTokenException(LockedException e) {
        log.warn("令牌认证错误: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handlerExceptions(Exception e) {
        log.error("系统异常:", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("系统繁忙，请稍后再试");
    }
}
