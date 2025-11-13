package com.daniel.chat_practice.domain.user;

import reactor.core.publisher.Mono;

public interface IUserService {
    Mono<User> createUser(User user);
    Mono<User> switchConnectionStatus(Long userId, Boolean connected);
}
