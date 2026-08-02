package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockScanner {

    public static List<BlockPos> findTargetOres(BotContext ctx, int radius) {
        Minecraft client = Minecraft.getInstance();
        List<BlockPos> validOres = new ArrayList<>();
        if (client.level == null || client.player == null) return validOres;

        BlockPos playerPos = client.player.blockPosition();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = client.level.getBlockState(pos);

                    if (isTargetOre(state)) {
                        validOres.add(pos);
                    }
                }
            }
        }

        // Sort closest to player
        validOres.sort(Comparator.comparingDouble(p -> p.distSqr(client.player.blockPosition())));
        return validOres;
    }

    private static boolean isTargetOre(BlockState state) {
        // Dwarven Mines Mithril & Titanium ores
        return state.is(Blocks.PRISMARINE) ||
               state.is(Blocks.DARK_PRISMARINE) ||
               state.is(Blocks.PRISMARINE_BRICKS) ||
               state.is(Blocks.CYAN_TERRACOTTA) ||
               state.is(Blocks.LIGHT_BLUE_WOOL) ||
               state.is(Blocks.BLUE_WOOL) ||
               state.is(Blocks.GRAY_WOOL) ||
               state.is(Blocks.SMOOTH_QUARTZ) ||
               state.is(Blocks.QUARTZ_BLOCK);
    }
}
