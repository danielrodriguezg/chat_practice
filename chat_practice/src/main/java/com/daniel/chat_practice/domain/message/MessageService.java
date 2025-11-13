package com.daniel.chat_practice.domain.message;

import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiverRepository;
import com.daniel.chat_practice.domain.chat.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService implements IMessageService {
    //Reactive repositories
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ChatReceiverRepository chatReceiverRepository;

    //Live stream
    private final Sinks.Many<Message> liveMessageSink = Sinks.many().replay().latest();
    private final Flux<Message> sharedLiveMessageFlux = liveMessageSink.asFlux()
            .cache();

    private final Sinks.Many<Tuple2<Long, Boolean>> writingUsersSink = Sinks.many().replay().latest();
    private final Flux<Tuple2<Long, Boolean>> writingUsersFlux = writingUsersSink.asFlux();

    @Override
    public Mono<Void> sendMessage(Message message) {
        return validateChatAndUser(message)
                .flatMap(chatReceiver -> {
                    message.setWriting(false);
                    return messageRepository.save(message);
                })
                .doOnSuccess(n -> {
                    liveMessageSink.tryEmitNext(n);
                    log.info("Mensaje enviado {}", n);
                }).doOnSuccess(n -> {
                    Tuple2<Long, Boolean> mensaje = Tuples.of(n.getFromUserId(), Boolean.FALSE);
                    writingUsersSink.tryEmitNext(mensaje);
                })
                .then(Mono.empty());


    }

    @Override
    public Mono<Void> writingMessage(Message message) {
        return validateChatAndUser(message)
                .doOnSuccess( chatReceiver -> {
                    Tuple2<Long, Boolean> mensaje = Tuples.of(chatReceiver.getUserId(), Boolean.TRUE);
                    writingUsersSink.tryEmitNext(mensaje);
                    log.info("Usuario {} escribiendo...", chatReceiver);
                })
                .then(Mono.empty());
    }

    @Override
    public Flux<Tuple2<Long, Boolean>> getWritingUsersFlux() {
        return writingUsersFlux
                .groupBy(Tuple2::getT1)
                .flatMap(groupedFlux -> groupedFlux
                        .distinctUntilChanged(Tuple2::getT2)
                        .cache(1)
                );
    }

    @Override
    public Flux<Message> getChatroomMessageStream(ChatReceiver chatReceiver) {
        Flux<Message> historical = messageRepository.findByToChatIdOrderById(chatReceiver.getChatId())
                .filter(message -> !message.getWriting())
                .doOnNext(message -> {
                    log.info("Historical message {}", message.toString());
                })
                .onErrorResume(e -> {
                    log.error("Error al procesar mensaje en flujo historico: {}", e.getMessage());
                    return Flux.empty();
                });

        Flux<Message> liveMessages = sharedLiveMessageFlux
                .filter(msg -> msg.getToChatId().equals(chatReceiver.getChatId()) && !msg.getWriting())
                .doOnNext(message -> {
                    log.info("Live stream message {}", message.toString());
                })
                .onErrorContinue((e, msg) ->
                        log.error("Error al procesar mensaje {} en flujo en vivo: {}", msg, e.getMessage()));


        return chatReceiverRepository.findByChatIdAndUserId(chatReceiver.getChatId(), chatReceiver.getUserId())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException(String.format("Usuario %s no hace parte del chat %s", chatReceiver.getUserId(), chatReceiver.getChatId())))))
                .flatMapMany(receiver -> Flux.concat(historical, liveMessages)
                        .distinct());


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
