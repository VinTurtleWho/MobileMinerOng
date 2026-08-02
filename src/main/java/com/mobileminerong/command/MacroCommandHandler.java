package com.mobileminerong.command;

import com.mobileminerong.debug.DebugLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;


public class MacroCommandHandler {


    private static boolean debugMode = false;


    public static boolean handle(String message){

        if(!message.startsWith("!macro"))
            return false;


        String[] args = message.split(" ");

        Minecraft client = Minecraft.getInstance();


        if(client.player == null)
            return true;



        if(args.length >= 2){

            switch(args[1]){


                case "debug":


                    if(args.length >= 3 &&
                       args[2].equalsIgnoreCase("off")){


                        debugMode = false;


                        DebugLogger.info(
                            "SYSTEM",
                            "Debug disabled"
                        );


                        client.player.sendSystemMessage(
                            Component.literal(
                            "§c[MobileMinerOng] Debug OFF"
                            )
                        );


                    } else {


                        debugMode = true;


                        DebugLogger.info(
                            "SYSTEM",
                            "Debug enabled"
                        );


                        client.player.sendSystemMessage(
                            Component.literal(
                            "§a[MobileMinerOng] Debug ON"
                            )
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
