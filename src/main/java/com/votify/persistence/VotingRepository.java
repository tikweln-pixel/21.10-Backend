package com.votify.persistence;

import com.votify.entity.Voting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotingRepository extends JpaRepository<Voting, Long> {
}
