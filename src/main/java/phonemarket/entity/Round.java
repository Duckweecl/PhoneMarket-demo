package phonemarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Round {

    private Long id;

    private Long gameId;

    private Integer roundNo;

    private String status;

    private Integer expectedPlayerCount;

    private Integer submittedCount;

    private BigDecimal economyFactor;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}