package com.mobileminerong.command;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.state.MacroMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
                    case "mode":
                        if (args.length >= 3) {
                            if (args[2].equalsIgnoreCase("miner")) ctx.setMode(MacroMode.MINER);
                            else if (args[2].equalsIgnoreCase("combat")) ctx.setMode(MacroMode.COMBAT);
                            else if (args[2].equalsIgnoreCase("idle")) ctx.setMode(MacroMode.IDLE);
                            client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Mode set to " + args[2].toUpperCase()));
                        }
                        break;
                    case "mine":
                        if (args.length >= 3) {
                            ctx.setMiningTargetId(args[2]);
                            client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Mining target: " + args[2]));
                        }
                        break;
                    case "combat":
                        if (args.length >= 3) {
                            ctx.setCombatTargetId(args[2]);
                            client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Combat target: " + args[2]));
                        }
                        break;
                    case "comtool":
                        if (args.length >= 3) {
                            ctx.setCombatToolSlot(Integer.parseInt(args[2]));
                            client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Combat tool slot: " + args[2]));
                        }
                        break;
                    case "mintool":
                        if (args.length >= 3) {
                            ctx.setMiningToolSlot(Integer.parseInt(args[2]));
                            client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Mining tool slot: " + args[2]));
                        }
                        break;
                    case "target":
                        if (args.length >= 3) {
                            if (args[2].equalsIgnoreCase("player")) {
                                if (args.length >= 4 && args[3].equalsIgnoreCase("off")) {
                                    ctx.setTargetPlayer(null);
                                    ctx.setPendingPlayerSearch(false);
                                    client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] Player targeting OFF"));
                                } else {
                                    ctx.setCombatTargetType(BotContext.CombatTargetType.PLAYER);
                                    ctx.setPendingPlayerSearch(true);
                                    client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Combat targeting: PLAYER"));
                                }
                            } else if (args[2].equalsIgnoreCase("mob")) {
                                ctx.setCombatTargetType(BotContext.CombatTargetType.MOB);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Combat targeting: MOB"));
                            }
                        }
                        break;
                    case "addmob":
                    case "delmob":
                        String mobName = parseQuotedArgument(message);
                        if (mobName != null) {
                            if (args[1].equals("addmob")) {
                                ctx.getMobWhitelist().add(mobName);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Added to whitelist: " + mobName));
                            } else {
                                ctx.getMobWhitelist().remove(mobName);
                                client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] Removed from whitelist: " + mobName));
                            }
                        }
                        break;
                    case "clearmobs":
                        ctx.getMobWhitelist().clear();
                        client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] Mob whitelist cleared"));
                        break;
                    case "debug":
                        if (args.length >= 3 && args[2].equalsIgnoreCase("off")) {
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

    private static String parseQuotedArgument(String message) {
        int firstQuote = message.indexOf('"');
        int lastQuote = message.lastIndexOf('"');
        if (firstQuote != -1 && lastQuote != -1 && firstQuote != lastQuote) {
            return message.substring(firstQuote + 1, lastQuote);
        }
        return null;
    }

    public static boolean isDebugEnabled(){
        return debugMode;
    }
}
