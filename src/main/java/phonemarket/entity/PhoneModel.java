package phonemarket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhoneModel {

    private Long id;

    /*
     * 系统初始手机为 null；
     * 玩家研发手机保存对应的 game_round.id。
     */
    private Long roundId;

    /*
     * 系统初始手机为 null；
     * 玩家研发手机保存对应的 game_player.id。
     */
    private Long gamePlayerId;

    private String modelName;

    /*
     * SYSTEM：系统初始手机
     * PLAYER：玩家研发手机
     */
    private String modelType;

    /*
     * 系统初始手机使用固定代码；
     * 玩家手机通常为 null。
     */
    private String modelCode;

    private Integer screenLevel;
    private Integer processorLevel;
    private Integer bodyLevel;
    private Integer batteryLevel;
    private Integer storageLevel;
    private Integer cameraLevel;

    /*
     * 数据库自动计算，插入时不传。
     */
    private Integer totalGrade;

    private LocalDateTime createdAt;
}