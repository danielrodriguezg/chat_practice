package com.daniel.chat_practice.domain.chat;

import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiver;
import com.daniel.chat_practice.domain.chat.chatreceiver.ChatReceiverRepository;
import com.daniel.chat_practice.domain.user.User;
import com.daniel.chat_practice.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChatService implements IChatService {
    private final ChatRepository chatRepository;
    private final ChatReceiverRepository chatReceiverRepository;
    private final UserRepository userRepository;

    @Override
    public Mono<Chat> createChatRoom(Flux<User> userList) {
        return chatRepository.save(new Chat()).doOnSuccess(chat -> {
            userList.flatMap(user -> chatReceiverRepository.save(new ChatReceiver(chat.getId(), user.getId())));
        });
    }

    @Override
    public Mono<ChatReceiver> addMemberToChatRoom(ChatReceiver chatReceiver) {
        return chatRepository.findById(chatReceiver.getChatId())
                .flatMap(chat -> userRepository.findById(chatReceiver.getUserId()))
                .flatMap(chat -> chatReceiverRepository.save(chatReceiver));
    }
}
