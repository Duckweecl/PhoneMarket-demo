package phonemarket.cache;

/**
 * Redis 缓存名称统一放在这里，避免各个 Service 使用不同字符串。
 */
public final class CacheNames {

    public static final String ROOM_DETAILS = "roomDetails";
    public static final String ACTIVE_GAMES = "activeGames";

    private CacheNames() {
    }
}
