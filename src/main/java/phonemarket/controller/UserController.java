package phonemarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.CreateUserRequest;
import phonemarket.entity.AppUser;
import phonemarket.service.UserService;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/login/create")
    public AppUser create(@RequestBody CreateUserRequest request){
        return userService.create(request.getUsername());
    }



}
