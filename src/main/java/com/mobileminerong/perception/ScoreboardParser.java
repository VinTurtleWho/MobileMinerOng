package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class ScoreboardParser {

    public static List<String> getSidebarLines(Minecraft client) {
        if (client.level == null) return Collections.emptyList();
        return Collections.emptyList();
    }

    public static void updateZone(BotContext ctx) {
        if (ctx != null) {
            ctx.setCurrentZone("Unknown");
        }
    }
}
