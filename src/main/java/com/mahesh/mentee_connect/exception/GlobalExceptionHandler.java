package com.mahesh.mentee_connect.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static String redact(String auth) {
        if (auth == null) return null;
        return auth.length() > 24 ? auth.substring(0, 24) + "..." : auth;
    }

    private static String describe(HttpServletRequest req) {
        String qs = req.getQueryString();
        String uri = qs != null ? req.getRequestURI() + "?" + qs : req.getRequestURI();
        String method = req.getMethod();
        String remote = req.getRemoteAddr();
        String auth = redact(req.getHeader("Authorization"));
        String params;
        try {
            params = req.getParameterMap().entrySet().stream()
                    .limit(12)
                    .map(e -> e.getKey() + "=" + (e.getValue() != null && e.getValue().length > 0
                            ? (e.getValue()[0] != null && e.getValue()[0].length() < 64
                                ? e.getValue()[0]
                                : "<truncated>")
                            : ""))
                    .collect(Collectors.joining("&"));
        } catch (Exception ignore) { params = "<unreadable>"; }
        return method + " " + uri + " | remote=" + remote + " | auth=" + auth
                + (params.isEmpty() ? "" : " | params=" + params);
    }

    private Map<String, Object> body(int status, String error, String message, HttpServletRequest req) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("timestamp", Instant.now().toString());
        b.put("status", status);
        b.put("error", error);
        b.put("message", message);
        b.put("path", req.getRequestURI());
        b.put("uri", req.getRequestURI());
        b.put("method", req.getMethod());
        return b;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("HTTP 404 on {} :: {}", describe(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(404, "Not Found", ex.getMessage(), req));
    }

    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(Exception ex, HttpServletRequest req) {
        log.warn("HTTP 401 on {} :: {}", describe(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(401, "Unauthorized", ex.getMessage(), req));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("HTTP 403 on {} :: {}", describe(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(403, "Forbidden", ex.getMessage(), req));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, HttpServletRequest req) {
        String msg = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException v) {
            msg = v.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        log.warn("HTTP 400 on {} :: {}", describe(req), msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(400, "Bad Request", msg, req));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
        log.warn("HTTP 404 on {} :: {}", describe(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(404, "Not Found", ex.getMessage(), req));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleAll(Throwable t, HttpServletRequest req) {
        log.error("HTTP 500 on {} :: {} :: {}",
                describe(req),
                t.getClass().getName(),
                t.getMessage(),
                t);
        String safeMessage = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        Map<String, Object> b = body(500, "Internal Server Error", safeMessage, req);
        b.put("exceptionType", t.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(b);
    }
}
