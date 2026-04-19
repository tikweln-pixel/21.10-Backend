package com.votify.service.factory.participant;

import com.votify.entity.Competitor;
import com.votify.entity.ParticipationRole;
import com.votify.entity.User;

/**
 * ConcreteCreator A del patrón Método Fábrica.
 *
 * Implementa el factory method para crear un Competitor.
 */
public class CompetitorCreator extends ParticipantCreator {

    @Override
    public User createUser(String name, String email) {
        return new Competitor(name, email, null);   // decide el tipo concreto
    }

    @Override
    public ParticipationRole getRole() {
        return ParticipationRole.COMPETITOR;
    }
}
