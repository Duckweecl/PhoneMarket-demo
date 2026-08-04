package phonemarket.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.entity.User;
import phonemarket.mapper.UserMapper;


@Service
public class UserService {
    private final UserMapper userMapper;
    public UserService(UserMapper userMapper){
        this.userMapper = userMapper;
    }
    public User create(String userName){
        User appuser = new User();
        appuser.setUsername(userName);
        userMapper.createUser(appuser);
        return appuser;
    }
    public void findUserId(long id){

        User user = userMapper.findUserId(id);
        if (user == null){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "用户不存在");
        }

    }
}
