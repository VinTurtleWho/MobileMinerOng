package com.mobileminerong.command;

import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.planning.task.DiagnosticTestTask;
import com.mobileminerong.MobileMinerClient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;


public class MacroCommandHandler {


    private static boolean debugMode = false;


    public static boolean handle(String message) {


        if(!message.startsWith("!macro")) {
            return false;
        }


        String[] args = message.split(" ");


        Minecraft client = Minecraft.getInstance();


        if(client.player == null)
            return true;



        if(args.length >= 2) {


            switch(args[1]) {


                case "debug":


                    if(args.length >= 3 &&
                       args[2].equalsIgnoreCase("off")) {


                        debugMode = false;


                        client.player.sendSystemMessage(
                            Component.literal(
                            "§c[MobileMinerOng] Debug disabled"
                            )
                        );


                    } else {


                        debugMode = true;


                        client.player.sendSystemMessage(
                            Component.literal(
                            "§a[MobileMinerOng] Debug enabled"
                            )
                        );


                        new DiagnosticTestTask()
                            .onStart(
                                MobileMinerClient.BOT_CONTEXT
                            );
                    }


                    break;



                default:

                    client.player.sendSystemMessage(
                        Component.literal(
                        "§eUnknown MobileMiner command"
                        )
                    );

            }

        }


        return true;
    }



    public static boolean isDebugEnabled(){

        return debugMode;

    }

}
