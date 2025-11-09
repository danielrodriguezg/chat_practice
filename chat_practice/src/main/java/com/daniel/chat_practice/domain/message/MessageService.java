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
import java.util.Comparator;

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
        //TODO: refactorizar para no crear sala ni relacion si no existen para
        // evitar envio de mensaje de usuarios que no hagan parte de la sala
        return chatRepository.findById(message.getToChatId())
                .switchIfEmpty(Mono.defer(() -> {
                    //TODO: se debe retornar error si no existe el chat
                    log.info("Creando nuevo chat");
                    return chatRepository.save(new Chat());
                })).flatMap(chat -> chatReceiverRepository.findByChatIdAndUserId(chat.getId(), message.getFromUserId())
                        .switchIfEmpty(Mono.defer(() -> {
                            // Crear la nueva entidad ChatReceiver
                            //TODO: se debe retornar error si no existe relacion
                            // (significa que el usuario no hace parte de la sala)
                            ChatReceiver newReceiver = new ChatReceiver(chat.getId(), message.getFromUserId());
                            log.info("Creando nueva relación ChatReceiver: {}", newReceiver);
                            return chatReceiverRepository.save(newReceiver);
                        }))).doOnError(throwable -> {
                    if (throwable instanceof DataIntegrityViolationException) {
                        if (throwable.getMessage().contains("chat_receiver_user_id_fkey"))
                            throw new RuntimeException("Usuario no existente");
                    }
                })
                .flatMap(chatReceiver -> {
                    message.setToChatId(chatReceiver.getChatId());
                    message.setSentAt(LocalDateTime.now());
                    return messageRepository.save(message);
                })
                .doOnSuccess(n -> {
                    liveMessageSink.tryEmitNext(n);
                    log.info("Mensaje creado {}", n.getContent());
                })
                .then(Mono.empty());


    }

    @Override
    public Flux<MessageDto> getChatroomMessageStream(ChatReceiver chatReceiver) {
        Flux<Message> historical = messageRepository.findByToChatIdOrderById(chatReceiver.getChatId())
                .doOnNext(message -> log.info("Historical message {}", message.toString()))
                .onErrorResume(e -> {
                    log.error("Error al procesar mensaje en flujo historico: {}", e.getMessage());
                    return Flux.empty();
                });

        Flux<Message> liveMessages = sharedLiveMessageFlux
                .filter(msg -> msg.getToChatId().equals(chatReceiver.getChatId()))
                .doOnNext(message -> log.info("Live stream message {}", message.toString()))
                .onErrorContinue((e, msg) ->
                        log.error("Error al procesar mensaje {} en flujo en vivo: {}", msg, e.getMessage()));

        // Definimos el nivel de concurrencia deseado para concatMap
        final int CONCURRENCY_LIMIT = 3;

        return chatReceiverRepository.findByChatIdAndUserId(chatReceiver.getChatId(), chatReceiver.getUserId())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException(String.format("Usuario %s no hace parte del chat %s", chatReceiver.getUserId(), chatReceiver.getChatId())))))
                .flatMapMany(receiver -> Flux.concat(historical, liveMessages)
                        .distinct(Message::getId)
                        .concatMap(message -> userRepository.findById(message.getFromUserId())
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.warn("Usuario {} inexistente", message.getFromUserId());
                                    return Mono.just(new User("Desconocido"));
                                })).flatMapMany(user -> Mono.just(new MessageDto(
                                        user.getNickname(),
                                        message.getSentAt(),
                                        message.getContent()
                                ))),CONCURRENCY_LIMIT));

    }

}
