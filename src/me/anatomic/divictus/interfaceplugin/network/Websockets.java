package me.anatomic.divictus.interfaceplugin.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.*;
import me.anatomic.divictus.interfaceplugin.InterfacePlugin;
import me.anatomic.divictus.interfaceplugin.Server.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;

public class Websockets {

    String path = "ws://localhost:8000/ws/server/chat/e28f6137-24f1-46ca-b655-093565a78d46/";

    Gson gson =  new GsonBuilder().create();
    public WebSocket ws = null; // actual websocket
    public boolean runningSocket = false;
    boolean runningListener = false;
    boolean wsPermitted = true;
    private Object IncomingTextFromWS;

    boolean isPermitted(){
        return wsPermitted;
    }

    public class MessageFromWS {
        private final String authorUUID;
        private final String text;

        public MessageFromWS(String authorUUID, String text) {
            System.out.println("Through here 2");
            this.authorUUID = authorUUID;
            this.text = text;
        }
    }
    public class inquiryFromWS {
        private final String ticket;
        private final String cmd;

        public inquiryFromWS(String ticket, String cmd) {
            System.out.println("Through here 2");
            this.ticket = ticket;
            this.cmd = cmd;
        }
    }

    public class IncomingTextFromWS {
        private final MessageFromWS message;
        private final inquiryFromWS inquiry;
        public IncomingTextFromWS(MessageFromWS message, inquiryFromWS inquiry){
            this.message = message;
            this.inquiry = inquiry;
        }
    }

    public void startWebSocket(InterfacePlugin context){
        (new Thread(new Runnable(){
            public void run(){
                int i = 0;
                while(true) {
                    if((ws == null || ws.getState() == WebSocketState.CLOSED || ws.getState() == null) && wsPermitted == true) {
                        try {
                            ws = new WebSocketFactory().createSocket(path, 5000).connect();
                            runningSocket = true;
                            System.out.println("[DivictusInterfacePlugin] connected to WebSocket.");
                            i = 0; // reset the counter of times it wasn't able to connect;
                        } catch (WebSocketException e) {
                            runningSocket = false;
//                            e.printStackTrace();
                            if(i<3){ // only display the message max 3 times and be done with it
                                System.out.println("[DivictusInterfacePlugin] failed to connect to the WebSocket.");
                                i++;
                                if (i==3){
                                    System.out.println("[DivictusInterfacePlugin] failed to connect. Won't be alerting anymore.");
                                    System.out.println("[DivictusInterfacePlugin] Retrying will continue silently.");
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if(!wsPermitted){
                        break;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        })).start();
    }

    public void startMainWebSocketListener(InterfacePlugin context){
        (new Thread(new Runnable(){
            public void run(){
                while(true) {
                    if(runningSocket && !runningListener) {
                        runningListener = true;
//                        System.out.println("Starting listener.");
                        initiateWebSocketListener(context);
                    } else if(!wsPermitted) {
                        break;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        })).start();
    }

    public void initiateWebSocketListener(InterfacePlugin context){
        ws.addListener(new WebSocketAdapter(){

            @Override
            public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                super.onConnectError(websocket, exception);
                runningListener = false;
                System.out.println("[DivictusInterfacePlugin] connection error.");

            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                super.onError(websocket, cause);
                System.out.println("[DivictusInterfacePlugin] thrown error.");

            }

            @Override
            public void onDisconnected(
                    WebSocket websocket,
                    WebSocketFrame serverCloseFrame,
                    WebSocketFrame clientCloseFrame,
                    boolean closedByServer
            ) throws Exception {
                super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
                System.out.println("[DivictusInterfacePlugin] disconnected from WebSocket.");

                runningSocket = false;
                runningListener = false;
            }

            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                super.onTextMessage(websocket, text);
                System.out.println(text);
                System.out.println("here");
                IncomingTextFromWS incoming = gson.fromJson(text, IncomingTextFromWS.class);
                System.out.println(incoming);
                if(incoming.message != null){
                    new SendMessage(UUID.fromString(incoming.message.authorUUID), incoming.message.text);
                } else if(incoming.inquiry != null){
                    if(Objects.equals(incoming.inquiry.cmd, "getOnlinePlayerCount")) {
                        Integer response = Bukkit.getServer().getOnlinePlayers().size();
                        InquiryResponse msg = new InquiryResponse(incoming.inquiry.ticket, Integer.toString(response));
                        ws.sendText(msg.jsonObj.toString());
                    }
                    if(Objects.equals(incoming.inquiry.cmd, "getOnlinePlayers")){
                        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
                        List<String> playerNames = new ArrayList<String>();
                        for(Player p: players){
                            playerNames.add(p.getName());
                        }
                        System.out.println(playerNames);
                        InquiryResponse msg = new InquiryResponse(incoming.inquiry.ticket, playerNames.toString());
                        ws.sendText(msg.jsonObj.toString());

                    }
                }
            }

        });
    }

    public boolean isWSRunning() {
        return runningSocket;
    }

}