package ru.practicum.main.request;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.Event;
import ru.practicum.main.event.EventRepository;
import ru.practicum.main.event.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.User;
import ru.practicum.main.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private static final String EVENT_NOT_FOUND = "Событие с id=%d не найдено.";
    private static final String USER_NOT_FOUND = "Пользователь с id=%d не найден.";

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public ParticipationRequestServiceImpl(ParticipationRequestRepository requestRepository,
                                           UserRepository userRepository,
                                           EventRepository eventRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(long userId) {
        ensureUserExists(userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto addRequest(long userId, long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format(USER_NOT_FOUND, userId)));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Нельзя подать заявку на свое событие.");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Событие должно быть опубликовано.");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос.");
        }
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников.");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setEvent(event);
        request.setRequester(requester);
        request.setCreated(LocalDateTime.now());
        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден."));
        request.setStatus(RequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    private void ensureUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format(USER_NOT_FOUND, userId));
        }
    }
}