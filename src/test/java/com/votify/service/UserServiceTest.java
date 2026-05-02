package com.votify.service;

import com.votify.dto.UserDto;
import com.votify.entity.User;
import com.votify.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UserService.
 *
 * Patrones testados:
 *   - Guard Clause: validaciones previas con mensajes descriptivos (ID nulo, email duplicado, etc.)
 *   - Service Layer: separación entre lógica de negocio y persistencia
 *
 * Cubre: register, login, findAll, findById, update, delete.
 *
 * Directorio GitHub: src/test/java/com/votify/service/UserServiceTest.java
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Tests unitarios")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Ana García", "ana@test.com", "pass123");
        user.setId(1L);
    }

    // ── register ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register → crea y devuelve UserDto cuando el email no está registrado")
    void register_createsAndReturnsDto_whenEmailIsNew() {
        when(userRepository.findByEmail("ana@test.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = service.register("Ana García", "ana@test.com", "pass123");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Ana García");
        assertThat(result.getEmail()).isEqualTo("ana@test.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register → lanza excepción cuando el email ya está registrado (Guard Clause)")
    void register_throwsException_whenEmailAlreadyExists() {
        when(userRepository.findByEmail("ana@test.com")).thenReturn(user);

        assertThatThrownBy(() -> service.register("Ana", "ana@test.com", "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está registrado");

        verify(userRepository, never()).save(any(User.class));
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login → devuelve UserDto cuando email y contraseña son correctos")
    void login_returnsUserDto_whenCredentialsAreCorrect() {
        when(userRepository.findByEmail("ana@test.com")).thenReturn(user);

        UserDto result = service.login("ana@test.com", "pass123");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("ana@test.com");
        assertThat(result.getName()).isEqualTo("Ana García");
    }

    @Test
    @DisplayName("login → lanza excepción cuando el usuario no existe (Guard Clause)")
    void login_throwsException_whenUserNotFound() {
        when(userRepository.findByEmail("noexiste@test.com")).thenReturn(null);

        assertThatThrownBy(() -> service.login("noexiste@test.com", "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("login → lanza excepción cuando la contraseña es incorrecta (Guard Clause)")
    void login_throwsException_whenPasswordIsWrong() {
        when(userRepository.findByEmail("ana@test.com")).thenReturn(user);

        assertThatThrownBy(() -> service.login("ana@test.com", "contraseñaIncorrecta"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Contraseña incorrecta");
    }

    // ── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → devuelve lista de UserDto con todos los usuarios")
    void findAll_returnsAllUsersAsDto() {
        User user2 = new User("Carlos López", "carlos@test.com", "abc");
        user2.setId(2L);
        when(userRepository.findAll()).thenReturn(List.of(user, user2));

        List<UserDto> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserDto::getEmail)
                .containsExactlyInAnyOrder("ana@test.com", "carlos@test.com");
    }

    @Test
    @DisplayName("findAll → devuelve lista vacía cuando no hay usuarios")
    void findAll_returnsEmpty_whenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDto> result = service.findAll();

        assertThat(result).isEmpty();
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById → devuelve UserDto cuando el usuario existe")
    void findById_returnsDto_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = service.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Ana García");
        assertThat(result.getEmail()).isEqualTo("ana@test.com");
    }

    @Test
    @DisplayName("findById → lanza excepción cuando el ID no existe")
    void findById_throwsException_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findById → lanza excepción cuando el ID es null (Guard Clause)")
    void findById_throwsException_whenIdIsNull() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).findById(any());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update → modifica nombre y email y devuelve DTO actualizado")
    void update_changesNameAndEmailAndReturnsUpdatedDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User updated = new User("Ana Martínez", "ana.martinez@test.com", "pass123");
        updated.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(updated);

        UserDto dto = new UserDto(1L, "Ana Martínez", "ana.martinez@test.com");
        UserDto result = service.update(1L, dto);

        assertThat(result.getName()).isEqualTo("Ana Martínez");
        assertThat(result.getEmail()).isEqualTo("ana.martinez@test.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("update → lanza excepción cuando el ID no existe")
    void update_throwsException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserDto dto = new UserDto(99L, "Nombre", "email@test.com");
        assertThatThrownBy(() -> service.update(99L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update → lanza excepción cuando el ID es null (Guard Clause)")
    void update_throwsException_whenIdIsNull() {
        UserDto dto = new UserDto(null, "Nombre", "email@test.com");

        assertThatThrownBy(() -> service.update(null, dto))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).findById(any());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete → llama a deleteById con el ID correcto")
    void delete_callsDeleteById() {
        service.delete(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete → lanza excepción cuando el ID es null (Guard Clause)")
    void delete_throwsException_whenIdIsNull() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).deleteById(any());
    }
}
