package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Showing one player weather of their own on a real server.
 *
 * <p>The server keeps the override itself and re-sends it when the player changes world or logs
 * back in, so a chip only has to say what somebody should see rather than keep saying it.
 */
@NullMarked
final class PlayerSkies {

    private PlayerSkies() {}

    /** Shows a player a sky, or gives them the world's own back. */
    static void show(Player player, Sky sky) {
        switch (sky) {
            case DOWNFALL -> player.setPlayerWeather(WeatherType.DOWNFALL);
            case CLEAR -> player.setPlayerWeather(WeatherType.CLEAR);
            case REAL -> player.resetPlayerWeather();
        }
    }
}
