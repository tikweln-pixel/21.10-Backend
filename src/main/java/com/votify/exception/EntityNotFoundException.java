package com.votify.exception;

/**
 * Excepción lanzada cuando una entidad no es encontrada en la base de datos.
 * Reemplaza RuntimeException genéricos para mejor manejo de errores.
 */
public class EntityNotFoundException extends RuntimeException {

	private final String entityName;
	private final Long entityId;

	public EntityNotFoundException(String entityName, Long entityId) {
		super(String.format("%s no encontrado con id: %d", entityName, entityId));
		this.entityName = entityName;
		this.entityId = entityId;
	}

	public EntityNotFoundException(String entityName, String identifier) {
		super(String.format("%s no encontrado: %s", entityName, identifier));
		this.entityName = entityName;
		this.entityId = null;
	}

	public String getEntityName() {
		return entityName;
	}

	public Long getEntityId() {
		return entityId;
	}
}
