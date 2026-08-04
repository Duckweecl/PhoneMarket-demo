package phonemarket.dto;

import lombok.Data;

@Data
public class Responce {
    private boolean success;
    private String nickname;
    private String username;
    private String password;
    private String message;
}

