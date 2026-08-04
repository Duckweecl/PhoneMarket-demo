package phonemarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class GamePageController {


    @GetMapping("/{username:[^.]+}")
    public String gamePage(@PathVariable String username) {
        return "forward:/game.html";
    }
}