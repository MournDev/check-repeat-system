package com.abin.checkrepeatsystem.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * XSS过滤：对请求参数中的脚本标签进行转义，防止反射型XSS攻击
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class XssFilter implements Filter {

    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script\\b[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data\\s*:\\s*text/html", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<form\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    private static class XssRequestWrapper extends HttpServletRequestWrapper {
        XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitize(value);
        }

        private String sanitize(String value) {
            if (value == null) return null;
            String result = value;
            for (Pattern pattern : XSS_PATTERNS) {
                result = pattern.matcher(result).replaceAll("");
            }
            return result;
        }
    }
}