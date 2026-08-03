package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StarBidDTO {
    private Long gamePlayerId;
    private String companyName;
    private BigDecimal bid;
}
