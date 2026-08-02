package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;

import java.util.Collection;

public class ScoreboardParser {

    public static void updateZone(BotContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (objective != null) {
            Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
            for (ScoreboardEntry entry : entries) {
                String name = entry.owner();
                if (name.contains("Zone:") || name.contains("㏿")) {
                    ctx.setCurrentZone(name.replaceAll("§[0-9a-fk-or]", "").trim());
                    return;
                }
            }
        }
    }
}
