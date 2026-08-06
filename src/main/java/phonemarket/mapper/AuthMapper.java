package phonemarket.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import phonemarket.entity.User;

@Mapper
public interface AuthMapper {

    @Insert("""
        INSERT INTO `user`
            (username, nickname, password_hash)
        VALUES
            (#{username}, #{nickname}, #{passwordHash})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int register(User user);

    @Select("""
        SELECT
            id,
            username,
            nickname,
            password_hash AS passwordHash,
            created_at AS createdAt
        FROM `user`
        WHERE username = #{username}
        LIMIT 1
        """)
    User findByUsername(@Param("username") String username);

    @Select("""
        SELECT
            id,
            username,
            nickname,
            password_hash AS passwordHash,
            created_at AS createdAt
        FROM `user`
        WHERE id = #{userId}
        LIMIT 1
        """)
    User findById(@Param("userId") long userId);

    @Update("""
        UPDATE `user`
        SET username = #{username}
        WHERE id = #{userId}
        """)
    int updateUsername(
            @Param("userId") long userId,
            @Param("username") String username
    );
}
