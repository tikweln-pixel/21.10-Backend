package com.votify.service.observer;

import com.votify.dto.ProjectRankingDto;
import com.votify.entity.Project;
import com.votify.entity.Voting;
import com.votify.persistence.ProjectRepository;
import com.votify.persistence.VotingRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingObserver implements VotoObserver {

    private final VotingRepository votingRepository;
    private final ProjectRepository projectRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RankingObserver(VotingRepository votingRepository,
                           ProjectRepository projectRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.votingRepository = votingRepository;
        this.projectRepository = projectRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onVotoGuardado(Voting voting) {
        if (voting.getCategory() == null) return;
        Long categoryId = voting.getCategory().getId();

        Map<Long, Double> scores = new HashMap<>();
        for (Object[] row : votingRepository.findProjectScoresByCategoryId(categoryId)) {
            scores.put((Long) row[0], ((Number) row[1]).doubleValue());
        }

        List<ProjectRankingDto> ranking = new ArrayList<>();
        for (Project p : projectRepository.findByCategoryId(categoryId)) {
            ranking.add(new ProjectRankingDto(
                    p.getId(), p.getName(),
                    scores.getOrDefault(p.getId(), 0.0).longValue()
            ));
        }
        ranking.sort((a, b) -> Long.compare(b.getTotalScore(), a.getTotalScore()));

        messagingTemplate.convertAndSend("/topic/ranking/" + categoryId, ranking);
    }
}
