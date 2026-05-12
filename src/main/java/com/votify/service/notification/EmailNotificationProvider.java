package com.votify.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación de NotificationProvider para notificaciones por email.
 * Esta es una implementación base que registra los emails en logs.
 * En un entorno de producción, se puede reemplazar con una implementación que
 * utilice un servicio real de email (como SendGrid, AWS SES, etc.).
 */
@Component
public class EmailNotificationProvider implements NotificationProvider {

	private static final Logger logger = LoggerFactory.getLogger(EmailNotificationProvider.class);
	private static final String SENDER_EMAIL = "noreply@votify.com";

	/**
	 * Envía una notificación por email (implementación base que utiliza logs).
	 * En producción, esta clase debe ser extendida para usar un servicio real de email.
	 *
	 * @param email Email del destinatario
	 * @param userName Nombre del usuario
	 * @param subject Asunto de la notificación
	 * @param message Cuerpo del mensaje
	 * @return true si se procesó correctamente
	 */
	@Override
	public boolean sendNotification(String email, String userName, String subject, String message) {
		try {
			if (email == null || email.isEmpty()) {
				logger.warn("No se puede enviar email: el destinatario no tiene email configurado");
				return false;
			}

			// Log simulando envío de email
			logger.info("====== EMAIL NOTIFICATION ======");
			logger.info("De: {}", SENDER_EMAIL);
			logger.info("Para: {}", email);
			logger.info("Asunto: {}", subject);
			logger.info("Usuario: {}", userName);
			logger.info("Mensaje:\n{}", buildEmailBody(userName, message));
			logger.info("================================");

			return true;
		} catch (Exception e) {
			logger.error("Error al procesar notificación para {}: {}", email, e.getMessage(), e);
			return false;
		}
	}

	@Override
	public String getProviderName() {
		return "EMAIL";
	}

	/**
	 * Construye el cuerpo del email con un formato amigable.
	 *
	 * @param userName Nombre del usuario
	 * @param message Mensaje principal
	 * @return Cuerpo del email formateado
	 */
	private String buildEmailBody(String userName, String message) {
		return "Hola " + userName + ",\n\n" +
				message + "\n\n" +
				"---\n" +
				"Este es un correo automático de Votify. Por favor no respondas a este email.\n" +
				"Si tienes preguntas, visita nuestro sitio web.";
	}
}

