package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.entity.AppUser;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;

@Mapper
public interface UserMapper {


    @Insert("""
            INSERT INTO user
            (username)
            VALUES
            (#{username})
            """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createUser(AppUser userName);

    @Select("""
            SELECT *
            FROM user
            WHERE id = #{id}
            """)
    @Options
    AppUser findUserId(long id);
    @Select("""
            SELECT *
            FROM user
            WHERE id = #{id}
            """)
    @Options
    AppUser findGamePlayerId(long id);

}

