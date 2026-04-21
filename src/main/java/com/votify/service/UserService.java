package com.votify.service;

import com.votify.dto.UserDto;
import com.votify.entity.User;
import com.votify.persistence.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDto> findAll() {
        List<User> users = userRepository.findAll();
        List<UserDto> result = new ArrayList<>();
        for (User user : users) {
            result.add(toDto(user));
        }
        return result;
    }

    public UserDto findById(Long id) {
        if (id == null) throw new RuntimeException("El ID del usuario no puede ser nulo");
        User user = userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        return toDto(user);
    }

    //Antigua implementacion de creacion de usuarios (cuando no teniamos implementado el login/registrer)
    public UserDto create(UserDto dto) {
        User user = new User(dto.getName(), dto.getEmail(), dto.getPassword());
        return toDto(userRepository.save(Objects.requireNonNull(user)));
    }

    //Metodo que permite crear/registrar un usuario nuevo
    public UserDto register (String name, String email, String password) {
        User existingUser = userRepository.findByEmail(email);
        //Comprobamos que el email no este registrado
        if (existingUser != null) {
            throw new RuntimeException("El email ya está registrado");
        }

        User newUser = new User(name, email, password);
        return toDto(userRepository.save(newUser));

    }

    //Metodo que hace la lógica de logueo de un usuario
    public UserDto login (String email, String password) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("Usuario no encontrado con el email: " + email);
        }

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        return toDto(user);
    }

    public UserDto update(Long id, UserDto dto) {
        if (id == null) throw new RuntimeException("El ID del usuario no puede ser nulo");
        User user = userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return toDto(userRepository.save(Objects.requireNonNull(user)));
    }

    public void delete(Long id) {
        if (id == null) throw new RuntimeException("El ID del usuario no puede ser nulo");
        userRepository.deleteById(Objects.requireNonNull(id));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }
}
