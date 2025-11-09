package com.daniel.chat_practice.domain.chat.chatreceiver;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatReceiverRepository extends R2dbcRepository<ChatReceiver, Void> {
    Flux<ChatReceiver> findByChatId(Long chatId);

    Flux<ChatReceiver> findByUserId(Long userId);

    // Método para encontrar una relación específica (sería el equivalente a findById)
    Mono<ChatReceiver> findByChatIdAndUserId(Long chatId, Long userId);

    // Para la operación de "borrar por ID compuesto"
    Mono<Void> deleteByChatIdAndUserId(Long chatId, Long userId);
}
