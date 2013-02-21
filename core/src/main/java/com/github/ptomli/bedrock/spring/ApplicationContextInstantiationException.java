package com.github.ptomli.bedrock.spring;

public class ApplicationContextInstantiationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ApplicationContextInstantiationException(String message) {
		super(message);
	}

	public ApplicationContextInstantiationException(Exception ex) {
		super(ex);
	}
}
