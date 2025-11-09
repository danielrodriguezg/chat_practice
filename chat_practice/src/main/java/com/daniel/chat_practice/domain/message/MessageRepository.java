package com.daniel.chat_practice.domain.message;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MessageRepository extends R2dbcRepository<Message, Long> {
    Flux<Message> findByToChatIdOrderById(Long chatId);
    Flux<Message> findByToChatIdAndIdLessThanOrderById(Long chatId, Long lastId);
}
