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
        if (client.level == null) return java.util.Collections.emptyList();

    }
}
