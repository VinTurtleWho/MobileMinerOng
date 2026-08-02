package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;

import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;


public class ScoreboardParser {


    public static List<String> getSidebarLines(Minecraft client) {

        return Collections.emptyList();

    }


    public static void updateZone(BotContext ctx) {

        Minecraft client = Minecraft.getInstance();


        if (ctx == null || client.level == null) {
            return;
        }


        String dimension =
                client.level.dimension()
                .toString();


        ctx.setCurrentZone(
                dimension
        );

    }

}
