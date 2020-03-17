package com.atguigu.gmall.search.controller;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
   public static final String DEFAULT_ERROR_VIEW = "error"; // 定义错误显示页，error.html

   @ExceptionHandler(Exception.class) // 所有的异常都是Exception子类
   public Object defaultErrorHandler(HttpServletRequest request, Exception e) {
      class ErrorInfo {
         private Integer code;//错误编码手册
         private String message;
         private String url;

         public Integer getCode() {
            return code;
         }

         public void setCode(Integer code) {
            this.code = code;
         }

         public String getMessage() {
            return message;
         }

         public void setMessage(String message) {
            this.message = message;
         }

         public String getUrl() {
            return url;
         }

         public void setUrl(String url) {
            this.url = url;
         }
      }
      ErrorInfo info = new ErrorInfo();
      info.setCode(100);     // 标记一个错误信息类型
      info.setMessage(e.getMessage());
      info.setUrl(request.getRequestURL().toString());
      return info;
   }
}