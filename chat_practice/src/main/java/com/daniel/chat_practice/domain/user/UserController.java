package com.daniel.chat_practice.domain.user;

import com.daniel.chat_practice.domain.chat.IChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final IUserService chatService;

    @MessageMapping("/create-user")
    public Mono<User> createUser(User user){
        return chatService.createUser(user);
    }

    @MessageMapping("/user/{userId}/connected/{connected}")
    public Mono<User> isUserConnected(@DestinationVariable Long userId, @DestinationVariable Boolean connected){
        return chatService.switchConnectionStatus(userId, connected);
    }
}
