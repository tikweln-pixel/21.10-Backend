package com.votify.service.factory.participant;

import com.votify.entity.ParticipationRole;
import com.votify.entity.User;
import com.votify.entity.Voter;

/**
 * ConcreteCreator B del patrón Método Fábrica.
 *
 * Implementa el factory method para crear un Voter.
 */
public class VoterCreator extends ParticipantCreator {

    @Override
    public User createUser(String name, String email) {
        return new Voter(name, email, null);         // decide el tipo concreto
    }

    @Override
    public ParticipationRole getRole() {
        return ParticipationRole.VOTER;
    }
}
