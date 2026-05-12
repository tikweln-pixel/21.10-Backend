package com.votify.service.notification;

/**
 * Interfaz para proveedores de notificaciones.
 * Permite diferentes implementaciones: email, SMS, push, etc.
 */
public interface NotificationProvider {

	/**
	 * Envía una notificación a un usuario.
	 *
	 * @param email Email del destinatario
	 * @param userName Nombre del usuario
	 * @param subject Asunto de la notificación
	 * @param message Cuerpo del mensaje
	 * @return true si la notificación se envió exitosamente, false en caso contrario
	 */
	boolean sendNotification(String email, String userName, String subject, String message);

	/**
	 * Obtiene el nombre del proveedor.
	 *
	 * @return Nombre del proveedor (ej: "EMAIL", "SMS", "PUSH")
	 */
	String getProviderName();
}
