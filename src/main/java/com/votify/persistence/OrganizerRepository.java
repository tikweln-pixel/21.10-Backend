package com.votify.persistence;

import com.votify.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrganizerRepository {

    private final UserRepository userRepository;

    public OrganizerRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User save(User organizer) {
        return userRepository.save(organizer);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
