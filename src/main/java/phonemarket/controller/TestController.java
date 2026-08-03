package phonemarket.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.service.TestService;

@RestController
public class TestController {
    private final TestService testService;
    public TestController(TestService testService){
        this.testService = testService;
    }

    @GetMapping("/T")
    public String test() {
        return testService.getString();
    }
}