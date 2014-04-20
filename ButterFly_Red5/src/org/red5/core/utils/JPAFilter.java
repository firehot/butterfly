package org.red5.core.utils;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.tools.JavaCompiler;

public class JPAFilter implements javax.servlet.Filter {

	@Override
	public void destroy() {
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		System.out.println("JPAFilter.doFilter() -- enter");
		JPAUtils.getEntityManager();
		chain.doFilter(request, response);
		JPAUtils.closeEntityManager();
		System.out.println("JPAFilter.doFilter() -- exiting");
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}

}
