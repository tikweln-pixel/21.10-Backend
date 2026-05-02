package com.votify.exception;

/**
 * Excepción lanzada cuando hay un error de lógica de negocio.
 * Para casos que no entran en EntityNotFoundException o ValidationException.
 */
public class BusinessException extends RuntimeException {

	private final String businessCode;

	public BusinessException(String message) {
		super(message);
		this.businessCode = null;
	}

	public BusinessException(String businessCode, String message) {
		super(message);
		this.businessCode = businessCode;
	}

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
		this.businessCode = null;
	}

	public String getBusinessCode() {
		return businessCode;
	}
}
