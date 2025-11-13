package com.daniel.chat_practice.domain.message;

import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface IMessageService {
    Mono<Void> sendMessage(Message message);
    Mono<Void> writingMessage(Message message);

    Flux<Tuple2<Long, Boolean>> getWritingUsersFlux();

    Flux<Message> getChatroomMessageStream(ChatReceiver chatReceiver);
}
