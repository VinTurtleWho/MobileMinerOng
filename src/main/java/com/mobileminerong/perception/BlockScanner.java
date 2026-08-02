package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockScanner {

    public static List<BlockPos> findTargetOres(BotContext ctx, int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<BlockPos> validOres = new ArrayList<>();
        if (client.world == null || client.player == null) return validOres;

        BlockPos playerPos = client.player.getBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = client.world.getBlockState(pos);

                    if (isTargetOre(state)) {
                        validOres.add(pos);
                    }
                }
            }
        }

        // Sort closest to player
        validOres.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(client.player.getPos())));
        return validOres;
    }

    private static boolean isTargetOre(BlockState state) {
        // Dwarven Mines Mithril & Titanium ores
        return state.isOf(Blocks.PRISMARINE) ||
               state.isOf(Blocks.DARK_PRISMARINE) ||
               state.isOf(Blocks.PRISMARINE_BRICKS) ||
               state.isOf(Blocks.CYAN_TERRACOTTA) ||
               state.isOf(Blocks.LIGHT_BLUE_WOOL) ||
               state.isOf(Blocks.BLUE_WOOL) ||
               state.isOf(Blocks.GRAY_WOOL) ||
               state.isOf(Blocks.SMOOTH_QUARTZ) ||
               state.isOf(Blocks.QUARTZ_BLOCK);
    }
}
