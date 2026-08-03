package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlayerRoundResultDTO {
    private Long gamePlayerId;
    private String companyName;
    private PhoneModelDTO phoneModel;

    private Integer productionQuantity;
    private Integer consumerSalesQuantity;
    private Integer unsoldQuantity;
    private BigDecimal salePrice;

    private Boolean filmAd;
    private Boolean onlineAd;
    private Boolean magazineAd;
    private BigDecimal starBid;
    private Boolean wonStar;

    private BigDecimal salesProfit;
}
