package phonemarket.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ActiveGamesResponse {
    private boolean success;
    private String message;
    private List<ActiveGameItem> games = new ArrayList<>();
}
