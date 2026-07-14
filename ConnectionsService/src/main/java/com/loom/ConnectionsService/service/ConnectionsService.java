package com.loom.ConnectionsService.service;

import com.loom.ConnectionsService.auth.AuthContextHolder;
import com.loom.ConnectionsService.entity.Person;
import com.loom.ConnectionsService.exception.BadRequestException;
import com.loom.ConnectionsService.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsService {

    private final PersonRepository personRepository;

    public List<Person> getFirstDegreeConnectionsOfUser(Long userId) {
        log.info("Getting first degree connections of user with ID: {}", userId);

        return personRepository.getFirstDegreeConnections(userId);
    }

    public void sendConnectionRequest(Long receiverId) {
        Long senderId = AuthContextHolder.getCurrentUserId();
        log.info("sending subscription request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both subscriber and writer are the same");
        }

        boolean alreadySubscribed = personRepository.alreadyConnected(senderId, receiverId);
        if (alreadySubscribed) {
            throw new BadRequestException("Already subscribed to this writer");
        }

        personRepository.addConnectionRequest(senderId, receiverId);
        log.info("Successfully subscribed");
    }

    public void acceptConnectionRequest(Long senderId) {
        throw new BadRequestException("Accepting connection is not supported on Substack");
    }

    public void rejectConnectionRequest(Long senderId) {
        Long receiverId = AuthContextHolder.getCurrentUserId();
        log.info("Unsubscribing from writer with id: {}", senderId);
        personRepository.rejectConnectionRequest(senderId, receiverId);
    }

    public Long getSubscriberCount(Long userId) {
        log.info("Getting subscriber count for user with ID: {}", userId);
        return personRepository.getSubscriberCount(userId);
    }
}
