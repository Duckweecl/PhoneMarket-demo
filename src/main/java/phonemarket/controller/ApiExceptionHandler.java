package phonemarket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.dto.ApiErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatusException(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "请求失败"
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ApiErrorResponse(false, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJson() {
        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(false, "请求内容格式错误"));
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException exception
    ) {
        String message = exception.getMessage() == null
                ? "当前操作无法完成"
                : exception.getMessage();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(false, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        exception.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(false, "服务器内部错误"));
    }
}
