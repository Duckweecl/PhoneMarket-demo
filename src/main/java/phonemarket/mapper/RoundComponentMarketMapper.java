package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.dto.ComponentMarketDTO;
import phonemarket.entity.RoundComponentMarket;
import phonemarket.settlement.ComponentMarketSettlementRow;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface RoundComponentMarketMapper {
    @Select("""
        SELECT id, round_id, component_type, component_level,
               base_price AS baseUnitPrice,
               supply_quantity, demand_quantity, premium_change,
               premium_factor, actual_unit_price
        FROM round_component_market
        WHERE round_id = #{roundId}
        ORDER BY component_type, component_level
        FOR UPDATE
        """)
    List<ComponentMarketSettlementRow> findSettlementRowsForUpdate(@Param("roundId") long roundId);

    @Update("""
        UPDATE round_component_market
        SET demand_quantity = #{demandQuantity},
            premium_change = #{premiumChange},
            premium_factor = #{premiumFactor},
            actual_unit_price = #{actualUnitPrice}
        WHERE round_id = #{roundId}
          AND component_type = #{componentType}
          AND component_level = #{componentLevel}
          AND premium_change IS NULL
        """)
    int updateSettlementResult(
            @Param("roundId") long roundId,
            @Param("componentType") String componentType,
            @Param("componentLevel") int componentLevel,
            @Param("demandQuantity") int demandQuantity,
            @Param("premiumChange") BigDecimal premiumChange,
            @Param("premiumFactor") BigDecimal premiumFactor,
            @Param("actualUnitPrice") BigDecimal actualUnitPrice
    );

    @Update("""
        UPDATE round_component_market
        SET next_supply_quantity = #{nextSupplyQuantity}
        WHERE round_id = #{roundId}
          AND component_type = #{componentType}
          AND component_level = #{componentLevel}
        """)
    int updateNextSupply(
            @Param("roundId") long roundId,
            @Param("componentType") String componentType,
            @Param("componentLevel") int componentLevel,
            @Param("nextSupplyQuantity") int nextSupplyQuantity
    );

    @Insert("""
        <script>
        INSERT INTO round_component_market (
            round_id, component_type, component_level, base_price,
            supply_quantity, demand_quantity, premium_change,
            premium_factor, actual_unit_price, next_supply_quantity
        ) VALUES
        <foreach collection="markets" item="market" separator=",">
            (#{market.roundId}, #{market.componentType}, #{market.componentLevel}, #{market.basePrice},
             #{market.supplyQuantity}, #{market.demandQuantity}, NULL,
             #{market.premiumFactor}, #{market.actualUnitPrice}, #{market.nextSupplyQuantity})
        </foreach>
        </script>
        """)
    int batchInsert(@Param("markets") List<RoundComponentMarket> markets);

    @Select("""
        SELECT component_type, component_level, base_price,
               supply_quantity, demand_quantity, premium_factor, actual_unit_price,
               next_supply_quantity
        FROM round_component_market
        WHERE round_id = #{roundId}
        ORDER BY component_type, component_level
        """)
    List<ComponentMarketDTO> findComponentDTOsByRoundId(@Param("roundId") long roundId);

    @Select("""
        SELECT actual_unit_price
        FROM round_component_market
        WHERE round_id = #{roundId}
          AND component_type = #{componentType}
          AND component_level = #{componentLevel}
        LIMIT 1
        """)
    BigDecimal findActualUnitPrice(
            @Param("roundId") long roundId,
            @Param("componentType") String componentType,
            @Param("componentLevel") int componentLevel
    );
}
