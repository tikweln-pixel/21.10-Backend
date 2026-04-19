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
        if (id == null) throw new RuntimeException("User ID cannot be null");
        User user = userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return toDto(user);
    }

    public UserDto create(UserDto dto) {
        User user = new User(dto.getName(), dto.getEmail());
        return toDto(userRepository.save(Objects.requireNonNull(user)));
    }

    public UserDto update(Long id, UserDto dto) {
        if (id == null) throw new RuntimeException("User ID cannot be null");
        User user = userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return toDto(userRepository.save(Objects.requireNonNull(user)));
    }

    public void delete(Long id) {
        if (id == null) throw new RuntimeException("User ID cannot be null");
        userRepository.deleteById(Objects.requireNonNull(id));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }
}
