package com.daniel.chat_practice.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{
    private final UserRepository userRepository;

    @Override
    public Mono<User> createUser(User user) {
        user.setId(null);
        return userRepository.save(user);
    }

    @Override
    public Mono<User> switchConnectionStatus(Long userId, Boolean connected) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("Usuario no existe"))))
                .flatMap(user ->{
                    user.setConnected(connected);
                    return userRepository.save(user);
                });
    }
}
