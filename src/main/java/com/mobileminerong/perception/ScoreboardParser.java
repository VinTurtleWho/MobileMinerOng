package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

public class ScoreboardParser {

    public static List<String> getSidebarLines(Minecraft client) {
        return Collections.emptyList();
    }


    public static void updateZone(BotContext ctx) {

        Minecraft client = Minecraft.getInstance();

        if (ctx == null || client.level == null || client.player == null) {
            return;
        }


        // Default vanilla information
        String dimension =
                client.level.dimension()
                .location()
                .toString();


        String biomeName = "Unknown";


        try {

            Biome biome =
                    client.level
                    .getBiome(
                        client.player.blockPosition()
                    )
                    .value();


            ResourceLocation key =
                    client.level
                    .registryAccess()
                    .registryOrThrow(
                        net.minecraft.core.registries.Registries.BIOME
                    )
                    .getKey(biome);


            if (key != null) {
                biomeName = key.toString();
            }


        } catch(Exception ignored) {

        }


        ctx.setCurrentZone(
                "Dimension: "
                + dimension
                + " | Biome: "
                + biomeName
        );
    }
}
