package phonemarket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Game {

    private Long id;
    private String status;
    private Integer currentRound;
    private Integer maxRound;
    private Integer playerCount;
    private LocalDateTime createdAt;
}