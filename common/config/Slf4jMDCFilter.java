package com.kpmg.rcm.sourcing.common.config;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Slf4jMDCFilter implements Filter {

	static int counter = 1;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		// UUID uniqueId = UUID.randomUUID();
		String url = ((HttpServletRequest) servletRequest).getRequestURL().toString();
		String queryString = ((HttpServletRequest) servletRequest).getQueryString();
		String reqId = url.replaceAll(".*?procure/", "").replaceAll("/", "-");
		String yr = "all";
		if (!StringUtils.isEmpty(queryString)) {
			yr = queryString.split("=")[1];
		}
		if (!reqId.contains("http")) {
			reqId = reqId + "-" + yr + "-" + counter++;
		}
		MDC.put("requestId", reqId);
		log.info("Request IP address is {}", servletRequest.getRemoteAddr());
		// log.info("Request content type is {}", servletRequest.getContentType());
		HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);
		filterChain.doFilter(servletRequest, responseWrapper);
		responseWrapper.setHeader("requestId", reqId);
		responseWrapper.copyBodyToResponse();
		log.info("Response header is set with uuid {}", responseWrapper.getHeader("requestId"));

	}
}