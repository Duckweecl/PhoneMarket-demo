package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.entity.PhoneModel;

import java.util.List;

@Mapper
public interface PhoneModelMapper {
    @Select("""
        SELECT * FROM phone_model
        WHERE round_id = #{roundId} AND model_type = 'PLAYER'
        ORDER BY game_player_id
        """)
    List<PhoneModel> findPlayerModelsByRoundId(@Param("roundId") long roundId);

    @Select("SELECT * FROM phone_model WHERE model_type = 'SYSTEM'")
    List<PhoneModel> findSystemModels();

    @Select("SELECT * FROM phone_model WHERE id = #{id}")
    PhoneModel findById(@Param("id") long id);

    @Insert("""
        INSERT INTO phone_model (
            round_id, game_player_id, model_name, model_type, model_code,
            screen_level, processor_level, body_level,
            battery_level, storage_level, camera_level, created_at
        ) VALUES (
            #{roundId}, #{gamePlayerId}, #{modelName}, 'PLAYER', NULL,
            #{screenLevel}, #{processorLevel}, #{bodyLevel},
            #{batteryLevel}, #{storageLevel}, #{cameraLevel}, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlayerModel(PhoneModel phoneModel);
}
