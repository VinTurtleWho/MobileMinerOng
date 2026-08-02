package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Objective;

import java.util.Collection;

public class ScoreboardParser {

    public static void updateZone(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.world == null) return;

        Scoreboard scoreboard = client.world.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        if (objective != null) {
            Collection<PlayerScoreEntry> entries = scoreboard.getPlayerScores(objective);
            for (PlayerScoreEntry entry : entries) {
                String name = entry.owner();
                if (name.contains("Zone:") || name.contains("㏿")) {
                    ctx.setCurrentZone(name.replaceAll("§[0-9a-fk-or]", "").trim());
                    return;
                }
            }
        }
    }
}
