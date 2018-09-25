package Main;

import javafx.application.Application;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;

/**
 * Created by David on 14/03/2018.
 */
public class Connection{


    static Socket conn = null;
    static PrintWriter output = null;
    static BufferedReader messageRecieved = null;

    static void openSocket() {
        try {
            conn = new Socket(Settings.HOST, Settings.PORT);
            output = new PrintWriter(conn.getOutputStream(), true);
            messageRecieved = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            output.print("PASS " + Settings.PASS + "\r\n");
            output.flush();
            output.print("NICK " + Settings.NICK + "\r\n");
            output.flush();
            output.print("JOIN #" + Settings.CHANNEL + "\r\n");
            output.flush();
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + Settings.HOST);
        } catch (IOException e) {
            System.err.println("Couldn't connect to " + Settings.HOST);
        }
    }

    static void sendMessage(String message) {
        output.print("PRIVMSG #" + Settings.CHANNEL + " :" + message + "\r\n");
        output.flush();
        //System.out.println("Sent: PRIVMSG #" + Settings.CHANNEL + ": " + message + "\r\n");
    }

    public static void main(String[] args) {
        Settings.setupSounds();
        if (args.length > 0) {
            //launch -a commandName location accesstype cooldown (s)
            //launch -e commandName location accesstype cooldown (s)
            //launch -d commandName
            if (args[0].equals("-a")) {
                try {
                    Sound s = new Sound(args[1], args[2], args[3], Integer.parseInt(args[4]));
                    Settings.soundList.add(s);
                    Settings.addNewSound(s);
                    System.out.println("Successfully added: " + args[1]);
                    //for(Sound v: Settings.soundList)System.out.println(v.getSoundCommand());
                    Thread.sleep(500);
                    System.exit(1);

                } catch (Exception e) {
                    System.err.println("Please use standard format:\nE.g: -a !commandTest \"C:\\...\\commandTest.mp3\" moderator 5\n");
                }
            } else if (args[0].equals("-e")) {
                for (Sound s : Settings.soundList) {
                    if (args[1].equals(s.getSoundCommand())) {
                        s.setFilePath(args[2]);
                        s.setAccessType(args[3]);
                        s.setCooldown(Integer.parseInt(args[4]));
                        Settings.editSound(s);
                        System.out.println("Successfully editted: " + args[1]);
                        break;
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(1);

            } else if (args[0].equals("-d")) {
                for (Sound s : Settings.soundList) {
                    if (args[1].equals(s.getSoundCommand())) {
                        Settings.deleteSound(s.getSoundCommand());
                        System.out.println("Successfully deleted: " + args[1]);
                        break;
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(1);
            } else {
                Settings.CHANNEL = args[0];
                Settings.NICK = args[1];
                Settings.PASS = args[2];
                new Connection().launch();
            }
        }
        if (Settings.CHANNEL == null) {
            System.err.println("Please view the readme for setup!");
            System.exit(1);
        }


    }

    public void checkMods() {
        try {
            URL chatList = new URL("http://tmi.twitch.tv/group/user/" + Settings.CHANNEL + "/chatters");
            try (InputStream is = chatList.openStream()) {
                JSONParser parser = new JSONParser();
                JSONObject obj = (JSONObject) parser.parse(new InputStreamReader(is));
                JSONObject chatter = (JSONObject) obj.get("chatters");
                JSONArray moderators = (JSONArray) chatter.get("moderators");
                Iterator<String> iterator = moderators.iterator();
                while (iterator.hasNext()) {
                    String currentMod = iterator.next();
                    if (!Settings.modsList.contains(currentMod)) Settings.addNewMod(currentMod);
                }
            }
        } catch (Exception e) {

        }
    }

    public static int mainTimer = 0;

    Thread innerThreadTimerClock = new Thread(new Runnable() {
        @Override
        public void run() {
            while (0 < 1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mainTimer += 1;
            }
        }
    });

    public void launch() {
        joinRoom();
        if (!Settings.modsList.contains(Settings.CHANNEL)) Settings.addNewMod(Settings.CHANNEL);
        for (int i = 0; i < 9; i++) {
            try {
                String tempLine = messageRecieved.readLine();
                if (tempLine != null) {
                    System.out.println(tempLine);
                }
            } catch (Exception e) {
            }
        }
        innerThreadTimerClock.start();
        checkMods();
        while (true) {
            try {
                String tempLine = messageRecieved.readLine();
                if (tempLine != null) {
                    System.out.println(tempLine);
                    if (tempLine.equals("PING :tmi.twitch.tv")) {
                        output.print("PONG :tmi.twitch.tv\r\n");
                        System.out.println("PONGED");
                        checkMods();
                        continue;
                    }

                    String actualLine = tempLine.substring(tempLine.indexOf("#"));
                    String command = actualLine.substring(actualLine.indexOf("!")).replace(" ", "");
                    String userRequesting = tempLine.substring(1, tempLine.indexOf("!"));
                    //System.out.println(actualLine);
                    //System.out.println(userRequesting);
                    //System.out.println(command);
                    if (!Settings.usersCooldown.containsKey(userRequesting))
                        Settings.usersCooldown.put(userRequesting, -2-Settings.perUserCoolDown);

                    if (command.substring(0,command.indexOf("n")+1).equalsIgnoreCase("!usercooldown")) {
                        String cooldown = command.substring(13);
                        //System.out.println(cooldown);
                        if (cooldown.contains("default")) {
                            Settings.perUserCoolDown = Settings.defaultUserCoolDown;
                        } else {
                            Settings.perUserCoolDown = Integer.parseInt(cooldown);
                            sendMessage("Per User Cooldown has been set to: " + cooldown);
                        }

                    }
                    if (command.substring(0,command.indexOf("n")+1).equalsIgnoreCase("!globalcooldown")) {
                        for (String mod : Settings.modsList) {
                            if (userRequesting.equals(mod)) {
                                String cooldown = command.substring(15);
                                //System.out.println(cooldown);
                                if (cooldown.contains("default")) {
                                    for (Sound s : Settings.soundList) s.setCooldown(s.getDefaultCooldown());
                                    Settings.perUserCoolDown = Settings.defaultUserCoolDown;
                                } else {
                                    cooldown = cooldown.replace(" ", "");
                                    int newGlobalCoolDown = Integer.parseInt(cooldown);
                                    if(newGlobalCoolDown == 0) Settings.perUserCoolDown = 0;
                                    sendMessage("Global Cooldown has been set to: " + newGlobalCoolDown);
                                    for (Sound s : Settings.soundList) s.setCooldown(newGlobalCoolDown);
                                    break;
                                }
                            }
                        }
                    } else {
                        for (Sound s : Settings.soundList) {
                            if (command.equals(s.getSoundCommand())) {
                                if (s.getAccessType().equals("mod")) {
                                    for (String mod : Settings.modsList) {
                                        if (userRequesting.equals(mod)) {
                                            if ((mainTimer - s.getLastUsed()) >= s.getCooldown()) {
                                                if (mainTimer - Settings.usersCooldown.get(userRequesting) >= Settings.perUserCoolDown) {
                                                    Settings.usersCooldown.replace(userRequesting, mainTimer);
                                                    new SoundPlayer(s);
                                                }
                                            }
                                        }
                                        break;
                                    }
                                } else {
                                    if ((mainTimer - s.getLastUsed()) >= s.getCooldown()) {
                                        if (mainTimer - Settings.usersCooldown.get(userRequesting) >= Settings.perUserCoolDown) {
                                            Settings.usersCooldown.replace(userRequesting, mainTimer);
                                            new SoundPlayer(s);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void joinRoom() {
        openSocket();
        try {
            boolean loading = true;
            String readbuffer = "";
            while (loading) {
                readbuffer += messageRecieved.readLine();
                String[] lines = readbuffer.split("\n");
                for (String s : lines) {
                    //System.out.println(s);
                    loading = s.equals("End of /NAMES list");
                }
            }
            sendMessage("Successfully joined chat");
        } catch (IOException e) {
            System.err.println("Couldn't connect to " + Settings.HOST);
        }
    }
}
