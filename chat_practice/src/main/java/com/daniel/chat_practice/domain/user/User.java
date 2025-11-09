package com.daniel.chat_practice.domain.user;

import com.daniel.chat_practice.domain.user.role.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("\"user\"")
public class User {
    @Id
    private Long id;
    private String nickname;
    private String password;
    private List<Role> roles;
    public User(String nickname){
        this.nickname = nickname;
    }
}
