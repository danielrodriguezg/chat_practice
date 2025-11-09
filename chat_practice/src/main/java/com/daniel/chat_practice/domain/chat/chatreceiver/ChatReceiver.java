package com.daniel.chat_practice.domain.chat.chatreceiver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReceiver {
    private Long chatId;
    private Long userId;
}
