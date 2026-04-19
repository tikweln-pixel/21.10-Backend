package com.votify.service.factory.participant;

import com.votify.entity.ParticipationRole;
import com.votify.entity.User;
import com.votify.persistence.UserRepository;

/**
 * Creator abstracto del patrón Método Fábrica.
 *
 * Declara el factory method {@code createUser()} que cada subclase concreta
 * implementa para decidir qué subtipo de User instanciar.
 *
 * La operación {@code register()} usa el factory method internamente sin
 * conocer el tipo concreto que se va a crear.
 */
public abstract class ParticipantCreator {

    // Factory Method — las subclases deciden qué tipo concreto crear
    public abstract User createUser(String name, String email);

    public abstract ParticipationRole getRole();

    // Operación común que usa el factory method internamente
    public User register(String name, String email, UserRepository userRepository) {
        User existing = userRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        return userRepository.save(createUser(name, email));
    }
}
