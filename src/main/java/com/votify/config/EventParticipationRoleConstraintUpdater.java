package com.votify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps legacy databases in sync with ParticipationRole enum values.
 * Some existing schemas still have a CHECK constraint without ORGANIZER.
 */
@Component
public class EventParticipationRoleConstraintUpdater implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EventParticipationRoleConstraintUpdater.class);

    private final JdbcTemplate jdbcTemplate;

    public EventParticipationRoleConstraintUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE event_participations DROP CONSTRAINT IF EXISTS event_participations_role_check");
            jdbcTemplate.execute(
                    "ALTER TABLE event_participations " +
                    "ADD CONSTRAINT event_participations_role_check " +
                    "CHECK (role IN ('COMPETITOR','SPECTATOR','ORGANIZER'))"
            );
            log.info("event_participations_role_check actualizado para incluir ORGANIZER");
        } catch (Exception ex) {
            log.warn("No se pudo actualizar event_participations_role_check: {}", ex.getMessage());
        }
    }
}
