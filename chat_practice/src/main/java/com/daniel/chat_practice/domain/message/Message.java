package com.daniel.chat_practice.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    @Id
    private Long id;
    @Column("from_user_id")
    private Long fromUserId;
    @Column("to_chat_id")
    private Long toChatId;
    private Boolean writing;
    private String content;
    private LocalDateTime sentAt;
}
