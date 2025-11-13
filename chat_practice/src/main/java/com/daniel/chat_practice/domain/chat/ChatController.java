package com.daniel.chat_practice.domain.chat;

import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import com.daniel.chat_practice.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final IChatService chatService;

    @MessageMapping("/chat/create")
    public Mono<Chat> createChatRoom(Flux<User> users){
        return chatService.createChatRoom(users);
    }

    @MessageMapping("/chat/add/member")
    public Mono<ChatReceiver> addMemberToChat(ChatReceiver chatReceiver){
        return chatService.addMemberToChatRoom(chatReceiver);
    }
}
