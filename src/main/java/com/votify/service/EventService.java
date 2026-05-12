package com.votify.service;

import com.votify.dto.CategoryDto;
import com.votify.dto.EventDto;
import com.votify.dto.EventParticipationDto;
import com.votify.dto.ProjectDto;
import com.votify.dto.UserDto;
import com.votify.entity.Category;
import com.votify.entity.Event;
import com.votify.entity.Project;
import com.votify.entity.User;
import com.votify.persistence.CategoryCriterionPointsRepository;
import com.votify.persistence.CommentRepository;
import com.votify.persistence.EvaluacionRepository;
import com.votify.persistence.EventJuryRepository;
import com.votify.persistence.EventParticipationRepository;
import com.votify.persistence.EventRepository;
import com.votify.persistence.ProjectRepository;
import com.votify.persistence.UserRepository;
import com.votify.persistence.VotingRepository;
import com.votify.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventParticipationService eventParticipationService;
    private final EventJuryService eventJuryService;
    private final UserRepository userRepository;
    private final VotingRepository votingRepository;
    private final EventParticipationRepository eventParticipationRepository;
    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final EventJuryRepository eventJuryRepository;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepository,
                        EventParticipationService eventParticipationService,
                        EventJuryService eventJuryService,
                        UserRepository userRepository,
                        VotingRepository votingRepository,
                        EventParticipationRepository eventParticipationRepository,
                        CommentRepository commentRepository,
                        ProjectRepository projectRepository,
                        CategoryCriterionPointsRepository criterionPointsRepository,
                        EvaluacionRepository evaluacionRepository,
                        EventJuryRepository eventJuryRepository,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.eventParticipationService = eventParticipationService;
        this.eventJuryService = eventJuryService;
        this.userRepository = userRepository;
        this.votingRepository = votingRepository;
        this.eventParticipationRepository = eventParticipationRepository;
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.eventJuryRepository = eventJuryRepository;
        this.notificationService = notificationService;
    }

    public List<EventDto> findAll() {
        List<Event> events = eventRepository.findAll();
        List<EventDto> result = new ArrayList<>();
        for (Event event : events) {
            result.add(toDto(event));
        }
        return result;
    }

    public EventDto findById(Long id) {
        if (id == null) throw new RuntimeException("El ID del evento no puede ser nulo");
        Event event = eventRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));
        return toDto(event);
    }

    public EventDto create(EventDto dto) {
        List<CategoryDto> incoming = dto.getCategories();
        if (incoming == null || incoming.isEmpty()) {
            throw new RuntimeException("Se requiere al menos una categoría");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new RuntimeException("El nombre del evento es obligatorio");
        }

        Event event = new Event(dto.getName().trim());
        event.setTimeInitial(dto.getTimeInitial());
        event.setTimeFinal(dto.getTimeFinal());
        if (dto.getOrganizerId() != null) {
            Long orgId = dto.getOrganizerId();
            User organizer = userRepository.findById(Objects.requireNonNull(orgId))
                    .orElseThrow(() -> new RuntimeException("Usuario (organizador) no encontrado con id: " + orgId));
            event.setOrganizer(organizer);
        }
        event = eventRepository.save(Objects.requireNonNull(event));

        Category firstCategory = null;
        for (CategoryDto cd : incoming) {
            if (cd == null || cd.getName() == null || cd.getName().isBlank()) {
                continue;
            }
            Category category = new Category(cd.getName().trim(), event);
            category.setTimeInitial(dto.getTimeInitial());
            category.setTimeFinal(dto.getTimeFinal());
            if (cd.getVotingType() != null) {
                category.setVotingType(cd.getVotingType());
            }
            Integer reminderMinutes = resolveReminderMinutes(dto);
            if (reminderMinutes != null) {
                category.setReminderMinutes(reminderMinutes);
            }
            if (cd.getReminderMinutes() != null) {
                category.setReminderMinutes(cd.getReminderMinutes());
            }
            if (cd.getTotalPoints() != null) {
                category.setTotalPoints(cd.getTotalPoints());
            }
            if (cd.getMaxVotesPerVoter() != null) {
                category.setMaxVotesPerVoter(cd.getMaxVotesPerVoter());
            }
            event.getCategories().add(category);
            if (firstCategory == null) {
                firstCategory = category;
            }
        }
        if (event.getCategories().isEmpty()) {
            throw new RuntimeException("Se requiere al menos una categoría con nombre válido");
        }
        eventRepository.save(Objects.requireNonNull(event));

        // Notificar apertura de votaciones para cada categoría
        for (Category category : event.getCategories()) {
            notificationService.notifyVotingOpened(event, category);
        }

        Long creatorId = dto.getOrganizerId();
        if (creatorId != null && firstCategory != null) {
            eventParticipationService.registerCompetitor(
                    Objects.requireNonNull(event.getId()),
                    Objects.requireNonNull(creatorId),
                    Objects.requireNonNull(firstCategory.getId()));
        }

        if (dto.getJuryUserIds() != null) {
            for (Long juryUserId : dto.getJuryUserIds()) {
                if (juryUserId != null) {
                    eventJuryService.registerJury(event.getId(), juryUserId);
                }
            }
        }

        return toDto(event);
    }

    public EventDto createForOrganizer(Long organizerId, EventDto dto) {
        if (organizerId == null) throw new RuntimeException("El ID del organizador no puede ser nulo");
        User organizer = userRepository.findById(Objects.requireNonNull(organizerId))
                .orElseThrow(() -> new RuntimeException("Usuario (organizador) no encontrado con id: " + organizerId));

        Event event = organizer.createEvent(dto.getName(), dto.getTimeInitial(), dto.getTimeFinal());
        return toDto(eventRepository.save(Objects.requireNonNull(event)));
    }

    public EventDto update(Long id, EventDto dto) {
        if (id == null) throw new RuntimeException("El ID del evento no puede ser nulo");
        Event event = eventRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));
        event.setName(dto.getName());
        event.setTimeInitial(dto.getTimeInitial());
        event.setTimeFinal(dto.getTimeFinal());
        if (dto.getOrganizerId() != null) {
            Long orgId = dto.getOrganizerId();
            User organizer = userRepository.findById(Objects.requireNonNull(orgId))
                    .orElseThrow(() -> new RuntimeException("Usuario (organizador) no encontrado con id: " + orgId));
            event.setOrganizer(organizer);
        }
        return toDto(eventRepository.save(Objects.requireNonNull(event)));
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        if (id == null) throw new RuntimeException("El ID del evento no puede ser nulo");
        Event event = eventRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));

        List<Long> categoryIds = new ArrayList<>();
        for (Category cat : event.getCategories()) {
            categoryIds.add(cat.getId());
        }

        List<Long> projectIds = new ArrayList<>();
        for (Project p : event.getProjects()) {
            projectIds.add(p.getId());
        }

        // Delete criterion points for all event categories
        for (Long categoryId : categoryIds) {
            evaluacionRepository.deleteByCategoryId(categoryId);
            criterionPointsRepository.deleteByCategoryId(categoryId);
        }

        // Delete votings linked to event categories
        if (!categoryIds.isEmpty()) {
            votingRepository.deleteByCategoryIdIn(categoryIds);
        }

        // Delete event participations
        eventParticipationRepository.deleteByEventId(id);
        eventJuryRepository.deleteByEventId(id);

        // Delete comments on event projects
        if (!projectIds.isEmpty()) {
            commentRepository.deleteByProjectIdIn(projectIds);
        }

        // Nullify category FK on projects to avoid FK constraint when categories are cascade-deleted
        List<Project> projects = projectRepository.findByEventId(id);
        for (Project p : projects) {
            p.setCategory(null);
        }
        projectRepository.saveAll(projects);

        eventRepository.delete(event);
    }

    private static Integer resolveReminderMinutes(EventDto dto) {
        if (dto.getReminderMinutes() != null) {
            return dto.getReminderMinutes();
        }
        if (dto.getReminderHours() != null) {
            return dto.getReminderHours() * 60;
        }
        return null;
    }

    private EventDto toDto(Event event) {
        UserDto creatorDto = null;
        Long organizerId = null;
        if (event.getOrganizer() != null) {
            User org = event.getOrganizer();
            organizerId = org.getId();
            creatorDto = new UserDto(org.getId(), org.getName(), org.getEmail());
        }

        List<CategoryDto> categoryDtos = new ArrayList<>();
        for (Category cat : event.getCategories()) {
            categoryDtos.add(categoryToDto(cat));
        }

        List<UserDto> participantDtos = new ArrayList<>();
        for (EventParticipationDto p : eventParticipationService.getParticipationsByEvent(event.getId())) {
            participantDtos.add(new UserDto(p.getUserId(), p.getUserName(), p.getUserEmail()));
        }

        List<ProjectDto> projectDtos = new ArrayList<>();
        for (Project p : event.getProjects()) {
            List<Long> compIds = new ArrayList<>();
            for (User c : p.getCompetitors()) {
                compIds.add(c.getId());
            }
            projectDtos.add(new ProjectDto(p.getId(), p.getName(), p.getDescription(), event.getId(), compIds));
        }

        EventDto dto = new EventDto(
                event.getId(),
                event.getName(),
                event.getTimeInitial(),
                event.getTimeFinal(),
                creatorDto,
                categoryDtos
        );
        dto.setOrganizerId(organizerId);
        dto.setParticipants(participantDtos);
        dto.setProjects(projectDtos);
        return dto;
    }

    private CategoryDto categoryToDto(Category c) {
        Long eventId = c.getEvent() != null ? c.getEvent().getId() : null;
        return new CategoryDto(
                c.getId(),
                c.getName(),
                c.getVotingType(),
                c.getTimeInitial(),
                c.getTimeFinal(),
                eventId,
                c.getReminderMinutes(),
                c.getTotalPoints(),
                c.getMaxVotesPerVoter()
        );
    }
}

