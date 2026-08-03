package phonemarket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppUser {

    private Long id;
    private String username;
    private LocalDateTime createdAt;
}