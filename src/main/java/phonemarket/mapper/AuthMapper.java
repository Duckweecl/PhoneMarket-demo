package phonemarket.mapper;

import org.apache.coyote.Response;
import org.apache.ibatis.annotations.*;
import phonemarket.entity.User;

@Mapper
public interface AuthMapper {
    @Insert("""
    INSERT INTO user
    (username,nickname,password_hash)
    VALUES
    (#{username},#{nickname},#{passwordHash})
    """)

    @Options
    int register(User username);

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

    @Options
    User login(String username);

    @Select("""
    
    SELECT
    *
    FROM `user`
    WHERE id = #{userid)}
    LIMIT 1
    """)

    @Options
    User getCurrentUser(long userid);



}
