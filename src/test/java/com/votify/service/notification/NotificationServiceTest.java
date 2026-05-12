package com.votify.service.notification;

import com.votify.entity.Category;
import com.votify.entity.Event;
import com.votify.entity.User;
import com.votify.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

	@Mock
	private NotificationProvider notificationProvider;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private NotificationService notificationService;

	private Event testEvent;
	private Category testCategory;
	private List<User> testUsers;

	@BeforeEach
	void setUp() {
		testEvent = new Event("Evento de Prueba");
		testEvent.setId(1L);
		testEvent.setTimeInitial(new Date());
		testEvent.setTimeFinal(new Date(System.currentTimeMillis() + 3600000));

		testCategory = new Category("Categoría Test", testEvent);
		testCategory.setId(1L);
		testCategory.setTimeInitial(new Date());
		testCategory.setTimeFinal(new Date(System.currentTimeMillis() + 1800000));

		testUsers = new ArrayList<>();
		User user1 = new User("Usuario 1", "user1@test.com", "password");
		user1.setId(1L);
		User user2 = new User("Usuario 2", "user2@test.com", "password");
		user2.setId(2L);
		testUsers.add(user1);
		testUsers.add(user2);
	}

	@Test
	void testNotifyVotingOpened() {
		when(userRepository.findAll()).thenReturn(testUsers);
		when(notificationProvider.sendNotification(anyString(), anyString(), anyString(), anyString()))
				.thenReturn(true);

		notificationService.notifyVotingOpened(testEvent, testCategory);

		verify(notificationProvider, times(2)).sendNotification(
				anyString(),
				anyString(),
				contains("Categoría Test"),
				anyString()
		);
	}

	@Test
	void testNotifyVotingClosed() {
		when(userRepository.findAll()).thenReturn(testUsers);
		when(notificationProvider.sendNotification(anyString(), anyString(), anyString(), anyString()))
				.thenReturn(true);

		notificationService.notifyVotingClosed(testEvent, testCategory);

		verify(notificationProvider, times(2)).sendNotification(
				anyString(),
				anyString(),
				contains("ha cerrado"),
				anyString()
		);
	}

	@Test
	void testNotifyVotingOpenedWithNullEvent() {
		notificationService.notifyVotingOpened(null, testCategory);

		verify(notificationProvider, never()).sendNotification(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void testNotifyVotingOpenedWithNullCategory() {
		notificationService.notifyVotingOpened(testEvent, null);

		verify(notificationProvider, never()).sendNotification(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void testNotifyVotingOpenedWithUsersWithoutEmail() {
		User userWithoutEmail = new User("Usuario Sin Email", null, "password");
		userWithoutEmail.setId(3L);
		testUsers.add(userWithoutEmail);

		when(userRepository.findAll()).thenReturn(testUsers);
		when(notificationProvider.sendNotification(anyString(), anyString(), anyString(), anyString()))
				.thenReturn(true);

		notificationService.notifyVotingOpened(testEvent, testCategory);

		// Should only send to 2 users with valid emails
		verify(notificationProvider, times(2)).sendNotification(
				anyString(),
				anyString(),
				anyString(),
				anyString()
		);
	}
}
