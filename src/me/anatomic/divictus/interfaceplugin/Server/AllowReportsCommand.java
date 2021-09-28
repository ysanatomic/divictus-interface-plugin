package me.anatomic.divictus.interfaceplugin.Server;

import me.anatomic.divictus.interfaceplugin.network.ReportAbilityChange;
import me.anatomic.divictus.interfaceplugin.network.SetNoteRequest;
import me.anatomic.divictus.interfaceplugin.network.Websockets;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;

public class AllowReportsCommand implements CommandExecutor {

    Websockets wsC;
    public AllowReportsCommand(Websockets ws){
        wsC = ws;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /blockreports AnatomicGod
        if (args.length < 1){
            return false;
        }
        else {
            if(wsC.ws != null){
                String username = args[0];
                ReportAbilityChange note = new ReportAbilityChange(username, true);
                if(wsC == null || wsC.ws == null || !wsC.runningSocket){
                    sender.sendMessage("[*} There is no connection to the interface currently.");
                    return true;
                }
                wsC.ws.sendText(note.jsonObj.toString());
                sender.sendMessage("[***] " + args[0] + " allowed to report!");
            } else{
                Bukkit.getServer().dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "tellraw " + sender.getName() +
                                " {text:\"" + "No connection." + "\", \"color\": \"red\"}");
            }

            return true;
        }
    }
}
