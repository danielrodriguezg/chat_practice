package com.daniel.chat_practice.domain.message;

import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Controller
@RequiredArgsConstructor
public class MessageController {
    private final IMessageService messageService;

    @MessageMapping("/chat/send")
    public Mono<Void> sendMessage(Mono<Message> message){
        return message.flatMap(messageService::sendMessage);
    }

    @MessageMapping("/chat/writing")
    public Mono<Void> startWriting(Mono<Message> message){
        return message.flatMap(messageService::writingMessage);
    }

    @MessageMapping("/chat/stream")
    public Flux<Message> messageStream(ChatReceiver chat){
        return messageService.getChatroomMessageStream(chat);
    }

    @MessageMapping("/chat/writing-users")
    public Flux<Tuple2<Long, Boolean>> writingUsers(){
        return messageService.getWritingUsersFlux();
    }
}
