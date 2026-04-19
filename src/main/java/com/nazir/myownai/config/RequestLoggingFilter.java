package com.nazir.myownai.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        System.out.println("INCOMING " + httpRequest.getMethod() + " " + httpRequest.getRequestURI());

        chain.doFilter(request, response);

        System.out.println("OUTGOING " + httpRequest.getMethod() + " " + httpRequest.getRequestURI() + " STATUS " + httpResponse.getStatus());
    }
}