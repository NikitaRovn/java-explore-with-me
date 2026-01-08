package ru.practicum.main.request.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.enums.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.enums.RequestStatus;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.mapper.ParticipationRequestMapper;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.repository.ParticipationRequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.List;

import static ru.practicum.main.utility.Constant.EVENT_NOT_FOUND;
import static ru.practicum.main.utility.Constant.USER_NOT_FOUND;

@Service
@Transactional
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
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

        RequestStatus status;
        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest request = ParticipationRequestMapper.toEntity(
                event,
                requester,
                status);
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