package phonemarket.dto;

import lombok.Data;

@Data
public class RoundActionRequest {

    private String modelName;

    private Integer screenLevel;
    private Integer processorLevel;
    private Integer bodyLevel;
    private Integer batteryLevel;
    private Integer storageLevel;
    private Integer cameraLevel;

    private Integer productionQuantity;
    private Integer salePrice;

    private Boolean filmAd;
    private Boolean onlineAd;
    private Boolean magazineAd;

    private Long starBid;
}
