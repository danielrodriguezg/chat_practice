package com.daniel.chat_practice.domain.message;

import com.daniel.chat_practice.domain.chat.Chat;
import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import com.daniel.chat_practice.domain.user.User;
import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiverRepository;
import com.daniel.chat_practice.domain.chat.ChatRepository;
import com.daniel.chat_practice.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService implements IMessageService {
    //Reactive repositories
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ChatReceiverRepository chatReceiverRepository;
    private final UserRepository userRepository;

    //Live stream
    private final Sinks.Many<Message> liveMessageSink = Sinks.many().replay().latest();
    private final Flux<Message> sharedLiveMessageFlux = liveMessageSink.asFlux()
            .cache();

    public Mono<Void> sendMessage(Message message) {
        return validateChatAndUser(message)
                .flatMap(chatReceiver -> messageRepository.findById(message.getId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Mensaje {} no existe", message.getId());
                    return Mono.error(new RuntimeException(String.format("Mensaje %s no existe", message.getId())));
                }))
                .flatMap(message1 -> {
                    message1.setSentAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
                    message1.setWriting(false);
                    message1.setContent(message.getContent());
                    return messageRepository.save(message1);
                })
                .doOnSuccess(n -> {
                    liveMessageSink.tryEmitNext(n);
                    log.info("Mensaje enviado {}", n);
                })
                .then(Mono.empty());


    }

    @Override
    public Mono<Void> writingMessage(Message message) {
        return validateChatAndUser(message)
                .flatMap(chatReceiver -> {
                    message.setId(null);
                    message.setToChatId(chatReceiver.getChatId());
                    message.setSentAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
                    message.setWriting(true);
                    message.setContent(null);
                    return messageRepository.save(message);
                })
                .doOnSuccess( n -> {
                    liveMessageSink.tryEmitNext(n);
                    log.info("Mensaje escribiendo {}", n);
                })
                .then(Mono.empty());
    }
    private final java.util.Set<Long> completedMessageIds = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    @Override
    public Flux<Message> getChatroomMessageStream(ChatReceiver chatReceiver) {
        Flux<Message> historical = messageRepository.findByToChatIdOrderById(chatReceiver.getChatId())
                .filter(message -> !message.getWriting())
                .doOnNext(message -> {
                    completedMessageIds.add(message.getId());
                    log.info("Historical message {}", message.toString());
                })
                .onErrorResume(e -> {
                    log.error("Error al procesar mensaje en flujo historico: {}", e.getMessage());
                    return Flux.empty();
                });

        Flux<Message> liveMessages = sharedLiveMessageFlux
                .filter(msg -> msg.getToChatId().equals(chatReceiver.getChatId()) && !msg.getWriting())
                .doOnNext(message -> {
                    completedMessageIds.add(message.getId());
                    log.info("Live stream message {}", message.toString());
                })
                .onErrorContinue((e, msg) ->
                        log.error("Error al procesar mensaje {} en flujo en vivo: {}", msg, e.getMessage()));

        Flux<Message> writingFlux = sharedLiveMessageFlux
                .filter(msg -> {
                    // Lógica para mostrar el mensaje writing=true solo si el ID no está completado
                    if (msg.getWriting()) {
                        return !completedMessageIds.contains(msg.getId());
                    }
                    // No queremos que los mensajes writing=false pasen por aquí (ya están en liveMessages)
                    return false;
                });


        // Definimos el nivel de concurrencia deseado para concatMap
        final int CONCURRENCY_LIMIT = 3;

        return chatReceiverRepository.findByChatIdAndUserId(chatReceiver.getChatId(), chatReceiver.getUserId())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException(String.format("Usuario %s no hace parte del chat %s", chatReceiver.getUserId(), chatReceiver.getChatId())))))
                .flatMapMany(receiver -> {
                    Flux<Message> combinedLive = Flux.merge(writingFlux, liveMessages);

                    return Flux.concat(historical, combinedLive)
                            .groupBy(Message::getId) // Agrupa por ID
                            .flatMap(groupedFlux -> groupedFlux
                                            .scan((m1, m2) -> !m2.getWriting() ? m2 : m1)
                                            .distinctUntilChanged(m -> m.getId()+":"+m.getWriting())
                            );
                });
                       /*.concatMap(message -> userRepository.findById(message.getFromUserId())
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.warn("Usuario {} inexistente", message.getFromUserId());
                                    return Mono.just(new User("Desconocido"));
                                })).flatMapMany(user -> Mono.just(new MessageDto(
                                        user.getNickname(),
                                        message.getSentAt(),
                                        message.getContent()
                                ))),CONCURRENCY_LIMIT));*/

    }
    private Mono<ChatReceiver> validateChatAndUser(Message message){
        return chatRepository.findById(message.getToChatId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("chat {} no existe", message.getToChatId());
                    return Mono.error(new RuntimeException(String.format("Chat %s no existe", message.getToChatId())));
                })).flatMap(chat -> chatReceiverRepository.findByChatIdAndUserId(chat.getId(), message.getFromUserId())
                        .switchIfEmpty(Mono.defer(() -> {
                            log.info("Usuario {} no pertenece a chat", message.getFromUserId());
                            return Mono.error(new RuntimeException(String.format("Usuario %s no pertenece a chat", message.getFromUserId())));
                        }))).doOnError(throwable -> {
                    if (throwable instanceof DataIntegrityViolationException) {
                        if (throwable.getMessage().contains("chat_receiver_user_id_fkey"))
                            throw new RuntimeException("Usuario no existente");
                    }
                });
    }
}
