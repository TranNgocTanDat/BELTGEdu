package com.example.beprojectweb.exception;

import com.example.beprojectweb.dto.response.APIResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalException {
    //Bắt mọi lỗi ngoại lệ chưa biết
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<APIResponse> handlingException(Exception exception){
        APIResponse apiResponse = new APIResponse();
        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    // Bắt lỗi chủ động cho mình ném ra
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<APIResponse> handlingAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();
        APIResponse apiResponse = new APIResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setResult(errorCode.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    //Bắt lỗi valid
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<APIResponse> handlingValidException(MethodArgumentNotValidException exception){
        //Lấy Key từ message trong annotation validation
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode;
        try {
            //Key tồn tại gán Errorcode
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
            //Key không hợp lệ hoặc null, gán lỗi mặc định
            errorCode = ErrorCode.INVALID_KEY;
        }

        APIResponse apiResponse = new APIResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }
}
