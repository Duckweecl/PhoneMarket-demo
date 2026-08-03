package phonemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuickLoginResponse {

    private Long userId;
    private String username;
}
