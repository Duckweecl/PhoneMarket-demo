package phonemarket.service;


import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.dto.Responce;
import phonemarket.entity.User;
import phonemarket.mapper.AuthMapper;

import java.util.Objects;

@Service
public class AuthService {
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Responce login(Responce response){
        String username = response.getUsername();
        String password = response.getPassword();
        User user = authMapper.login(username);
        if (user == null){
            response.setSuccess(false);
            response.setMessage("登陆失败 用户名不存在/密码错误");
            return response;
        }
        if (!(passwordEncoder.matches(
                password,
                user.getPasswordHash()
        ))){
            response.setSuccess(false);
            response.setMessage("登陆失败 用户名不存在/密码错误");
        }
        else{
            response.setSuccess(true);
            response.setUserid(user.getId());
            response.setMessage("登陆成功");
            response.setNickname(user.getNickname());
        }
        return response;
    }

    public Responce getCurrentUser(Long userId) {

        Responce response = new Responce();

        User user = authMapper.getCurrentUser(userId);

        if (user == null) {
            response.setSuccess(false);
            response.setMessage("用户不存在");
            return response;
        }

        response.setSuccess(true);
        response.setMessage("用户已登录");
        response.setUserid(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        return response;
    }

    @Transactional
    public Responce register(Responce response){

        String username = response.getUsername();
        String nickname = response.getNickname();
        String password = response.getPassword();
        if (checkUsernameIsNull(nickname)) {
            response.setMessage("昵称不能为空");
            return response;
        }

        if (nickname.trim().length() < 2) {
            response.setMessage("昵称不能少于2个字符");
            return response;
        }

        if (checkUsernameIsTooLong(nickname)) {
            response.setMessage("昵称不能超过20个字符");
            return response;
        }

        if (checkNicknameFormatIsWrong(nickname)) {
            response.setMessage("昵称只能包含中文或英文");
            return response;
        }

        if (checkUsernameIsNull(username)) {
            response.setMessage("用户名不能为空");
            return response;
        }

        if (checkUsernameIsTooShort(username)) {
            response.setMessage("用户名不能少于4个字符");
            return response;
        }

        if (checkUsernameIsTooLong(username)) {
            response.setMessage("用户名不能超过20个字符");
            return response;
        }

        if (checkUsernameFormatIsWrong(username)) {
            response.setMessage("用户名只能包含字母、数字和下划线");
            return response;
        }

        if (checkPasswordIsNull(password)) {
            response.setMessage("密码不能为空");
            return response;
        }

        if (checkPasswordIsTooShort(password)) {
            response.setMessage("密码不能少于6个字符");
            return response;
        }

        if (checkPasswordIsTooLong(password)) {
            response.setMessage("密码不能超过50个字符");
            return response;
        }

        if (checkPasswordIsEqualToUsername(username, password)) {
            response.setMessage("密码不能与用户名相同");
            return response;
        }
        User user = new User();
        user.setNickname(response.getNickname());
        user.setUsername(response.getUsername());
        user.setPasswordHash(
                passwordEncoder.encode(response.getPassword())
        );

        System.out.println(user);
        try {
            int rows = authMapper.register(user);

            if (rows != 1) {
                response.setMessage("注册失败，请稍后重试");
                return response;
            }

        } catch (DuplicateKeyException exception) {
            response.setMessage("用户名已存在");
            return response;
        }


        response.setMessage("注册成功 请返回登陆");
        return response;
    }

    // 检查用户名是否为 null 或空字符串
    public boolean checkUsernameIsNull(String username) {
        return username == null || username.trim().isEmpty();
    }
    // 检查用户名是否少于 4 个字符
    public boolean checkUsernameIsTooShort(String username) {
        if (username == null) {
            return true;
        }

        return username.trim().length() < 4;
    }
    // 检查用户名是否超过 20 个字符
    public boolean checkUsernameIsTooLong(String username) {
        if (username == null) {
            return false;
        }

        return username.trim().length() > 20;
    }
    // 检查用户名是否包含非法字符
// 只允许英文字母、数字和下划线
    public boolean checkUsernameFormatIsWrong(String username) {
        if (username == null) {
            return true;
        }
        return !username.trim().matches("^[A-Za-z0-9_]+$");
    }

    // 只允许中文和英文字母，不允许空格
    public boolean checkNicknameFormatIsWrong(String username) {
        if (username == null) {
            return true;
        }

        return !username.matches("^[A-Za-z\\u4e00-\\u9fa5]+$");
    }

    // 检查密码是否为 null 或空字符串
    public boolean checkPasswordIsNull(String password) {
        return password == null || password.isEmpty();
    }

    // 检查密码是否少于 6 个字符
    public boolean checkPasswordIsTooShort(String password) {
        return password.length() < 6;
    }

    // 检查密码是否超过 50 个字符
    public boolean checkPasswordIsTooLong(String password) {
        return password.length() > 50;
    }

    public boolean checkPasswordIsEqualToUsername(String username,String password) {
        return Objects.equals(password, username);
    }




}
