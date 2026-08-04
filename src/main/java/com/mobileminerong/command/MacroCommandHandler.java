package com.mobileminerong.command;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.debug.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class MacroCommandHandler {

    private static boolean debugMode = false;

    public static boolean handle(String message, BotContext ctx) {
        if (!message.startsWith("!macro")) return false;

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            String[] args = message.split(" ");
            if (client.player == null) return;

            if (args.length >= 2) {
                switch (args[1]) {
                    case "target":
                        if (args.length >= 3 && args[2].equalsIgnoreCase("player")) {
                            if (args.length >= 4 && args[3].equalsIgnoreCase("off")) {
                                ctx.setTargetPlayer(null);
                                ctx.setPendingPlayerSearch(false);
                                client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] Player targeting OFF"));
                            } else {
                                ctx.setPendingPlayerSearch(true);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Searching for nearest player..."));
                            }
                        break;
                    case "debug":
                        // ... (keep existing debug logic)
                        if(args.length >= 3 && args[2].equalsIgnoreCase("off")){
                            debugMode = false;
                            DebugLogger.info("SYSTEM", "Debug disabled");
                            client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] Debug OFF"));
                        } else {
                            debugMode = true;
                            DebugLogger.info("SYSTEM", "Debug enabled");
                            client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Debug ON"));
                        }
                        break;
                    default:
                        client.player.sendSystemMessage(Component.literal("§eUnknown MobileMiner command"));
                }
            }
        });
        return true;
    }



    public static boolean isDebugEnabled(){

        return debugMode;

    }

}
