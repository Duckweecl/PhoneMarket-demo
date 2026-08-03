package phonemarket.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import phonemarket.dto.QuickLoginUser;

@Mapper
public interface QuickLoginMapper {

    @Select("""
        SELECT id
        FROM `user`
        WHERE username = #{username}
        LIMIT 1
        """)
    Long findIdByUsername(
            @Param("username") String username
    );

    @Insert("""
        INSERT INTO `user` (username)
        VALUES (#{username})
        """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "id"
    )
    int insertUser(QuickLoginUser user);
}
