package com.votify.mapper;

import com.votify.dto.VotingDto;
import com.votify.entity.Voting;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre Voting (entidad) y VotingDto (DTO).
 * Centraliza la lógica de transformación y elimina duplicación en servicios.
 */
@Component
public class VotingMapper {

	/**
	 * Convierte una entidad Voting a VotingDto
	 */
	public VotingDto toDto(Voting voting) {
		if (voting == null) {
			return null;
		}

		Long categoryId  = voting.getCategory()  != null ? voting.getCategory().getId()  : null;
		Long voterId     = voting.getVoter()     != null ? voting.getVoter().getId()     : null;
		Long projectId   = voting.getProject()   != null ? voting.getProject().getId()   : null;
		Long criterionId = voting.getCriterion() != null ? voting.getCriterion().getId() : null;

		VotingDto dto = new VotingDto(
				voting.getId(),
				voterId,
				projectId,
				criterionId,
				voting.getScore(),
				categoryId,
				voting.getManuallyModified()
		);

		dto.setComentario(voting.getComentario());
		dto.setWeightedScore(voting.getWeightedScore());
		dto.setWeightingStrategy(voting.getWeightingStrategy());

		return dto;
	}
}
