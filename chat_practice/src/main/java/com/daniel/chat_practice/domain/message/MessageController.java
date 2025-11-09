package com.daniel.chat_practice.domain.message;

import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class MessageController {
    private final IMessageService chatService;

    @MessageMapping("chat.send")
    public Mono<Void> sendMessage(Mono<Message> message){
        return message.flatMap(chatService::sendMessage);
    }

    @MessageMapping("chat.stream")
    public Flux<MessageDto> messageStream(ChatReceiver chat){
        return chatService.getChatroomMessageStream(chat);
    }
}
