package com.daniel.chat_practice.domain.chat;

import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import com.daniel.chat_practice.domain.user.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IChatService {
    Mono<Chat> createChatRoom(Flux<User> userList);
    Mono<ChatReceiver> addMemberToChatRoom(ChatReceiver chatReceiver);
}
