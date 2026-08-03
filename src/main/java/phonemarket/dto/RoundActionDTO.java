package phonemarket.dto;

import lombok.Data;

@Data
public class RoundActionDTO {

    private Integer productionQuantity;
    private Integer salePrice;

    private Boolean filmAd;
    private Boolean onlineAd;
    private Boolean magazineAd;

    private Long starBid;
}
