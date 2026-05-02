package com.votify.exception;

/**
 * Excepción lanzada cuando una validación de negocio falla.
 * Diferencia entre errores de validación y errores de sistema.
 */
public class ValidationException extends RuntimeException {

	private final String field;
	private final String reason;

	public ValidationException(String field, String reason) {
		super(String.format("Validación fallida en '%s': %s", field, reason));
		this.field = field;
		this.reason = reason;
	}

	public ValidationException(String message) {
		super(message);
		this.field = null;
		this.reason = message;
	}

	public String getField() {
		return field;
	}

	public String getReason() {
		return reason;
	}
}
