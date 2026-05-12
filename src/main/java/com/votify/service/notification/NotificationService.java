package com.votify.service.notification;

import com.votify.entity.Event;
import com.votify.entity.Category;
import com.votify.entity.User;
import com.votify.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Servicio de notificaciones para eventos de votación.
 * Maneja el envío de notificaciones sobre apertura y cierre de votaciones.
 */
@Service
public class NotificationService {

	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationProvider notificationProvider;
	private final UserRepository userRepository;

	public NotificationService(NotificationProvider notificationProvider, UserRepository userRepository) {
		this.notificationProvider = notificationProvider;
		this.userRepository = userRepository;
	}

	/**
	 * Envía notificaciones a todos los participantes cuando se abre una votación.
	 *
	 * @param event Evento en el que se abre la votación
	 * @param category Categoría en la que se abre la votación
	 */
	public void notifyVotingOpened(Event event, Category category) {
		if (event == null || category == null) {
			logger.warn("No se puede enviar notificación: event o category es null");
			return;
		}

		String subject = "¡La votación de \"" + category.getName() + "\" ha abierto! 🗳️";
		String message = buildVotingOpenedMessage(event, category);

		List<User> participantUsers = userRepository.findAll();

		for (User user : participantUsers) {
			if (user.getEmail() != null && !user.getEmail().isEmpty()) {
				boolean sent = notificationProvider.sendNotification(
						user.getEmail(),
						user.getName(),
						subject,
						message
				);

				if (!sent) {
					logger.warn("No se envió la notificación de apertura de votación al usuario: {}", user.getId());
				}
			}
		}
	}

	/**
	 * Envía recordatorios a todos los participantes cuando se cierra una votación.
	 *
	 * @param event Evento en el que se cierra la votación
	 * @param category Categoría en la que se cierra la votación
	 */
	public void notifyVotingClosed(Event event, Category category) {
		if (event == null || category == null) {
			logger.warn("No se puede enviar recordatorio: event o category es null");
			return;
		}

		String subject = "⏱️ La votación de \"" + category.getName() + "\" ha cerrado";
		String message = buildVotingClosedMessage(event, category);

		List<User> participantUsers = userRepository.findAll();

		for (User user : participantUsers) {
			if (user.getEmail() != null && !user.getEmail().isEmpty()) {
				boolean sent = notificationProvider.sendNotification(
						user.getEmail(),
						user.getName(),
						subject,
						message
				);

				if (!sent) {
					logger.warn("No se envió el recordatorio de cierre de votación al usuario: {}", user.getId());
				}
			}
		}
	}

	/**
	 * Construye el mensaje para la notificación de apertura de votación.
	 */
	private String buildVotingOpenedMessage(Event event, Category category) {
		StringBuilder message = new StringBuilder();
		message.append("¡La votación para la categoría \"").append(category.getName()).append("\"");
		message.append(" del evento \"").append(event.getName()).append("\" ha abierto!\n\n");

		if (category.getTimeFinal() != null) {
			message.append("Fecha de cierre: ").append(formatDate(category.getTimeFinal())).append("\n\n");
		}

		message.append("Puedes emitir tu voto en la plataforma de Votify.\n");
		message.append("¡No pierdas la oportunidad de votar!");

		return message.toString();
	}

	/**
	 * Construye el mensaje para la notificación de cierre de votación.
	 */
	private String buildVotingClosedMessage(Event event, Category category) {
		StringBuilder message = new StringBuilder();
		message.append("La votación para la categoría \"").append(category.getName()).append("\"");
		message.append(" del evento \"").append(event.getName()).append("\" ha cerrado.\n\n");

		message.append("Los resultados estarán disponibles pronto en la plataforma.\n");
		message.append("Gracias por tu participación.");

		return message.toString();
	}

	/**
	 * Formatea una fecha para visualización en el email.
	 */
	private String formatDate(Date date) {
		if (date == null) {
			return "No especificada";
		}

		LocalDateTime dateTime = date.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();

		return String.format("%02d/%02d/%d a las %02d:%02d",
				dateTime.getDayOfMonth(),
				dateTime.getMonthValue(),
				dateTime.getYear(),
				dateTime.getHour(),
				dateTime.getMinute());
	}
}
