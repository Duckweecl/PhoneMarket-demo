package phonemarket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    private String nickname;
    private String passwordHash;
    private LocalDateTime createdAt;
}