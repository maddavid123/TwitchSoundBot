package madd.TwitchBot;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Connect to Twitch chat via IRC, and manage all communication.
 */
final class TwitchInterface {
    /**
     * Handle all outgoing messages to twitch IRC.
     */
    private static PrintWriter messageToTwitch = null;

    /**
     * Handle all incoming messages from twitch IRC.
     */
    private static BufferedReader messageFromTwitch = null;

    /**
     * Count the number of seconds passed since the launch of the program.
     */
    private static int mainTimer = 0;

    /**
     * Determine whether the primary timer should end.
     * Primary use within INNER_THREAD_TIMER_CLOCK.
     */
    private static boolean programRunning = true;

    /**
     * Secondary Thread running all timer related aspects.
     */
    private static final Thread INNER_THREAD_TIMER_CLOCK = new Thread(() -> {
        while (programRunning) {
            try {
                Thread.sleep(1000); // One second.
                checkMods();
            } catch (InterruptedException e) {
                System.out.println("Critical Error in timing Thread, shutting down.");
                System.exit(0);
            }
            mainTimer += 1;
        }
    });

    /**
     * Utilising messageToTwitch to send a message to Twitch IRC.
     * @param message the message to be sent out to Twitch IRC.
     */
    private static void sendMessage(final String message) {
        messageToTwitch.print("PRIVMSG #" + Settings.getChannelName()
                + " :" + message + "\r\n");
        messageToTwitch.flush();
    }

    /**
     * Opens a socket and attempts to connect to Twitch IRC.
     */
    private static void openSocket() {
        try {
            Socket conn = new Socket(Settings.getHOST(), Settings.getPORT());
            messageToTwitch = new PrintWriter(conn.getOutputStream(), true);
            messageFromTwitch = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            messageToTwitch.print("PASS " + Settings.getAuthCode() + "\r\n");
            messageToTwitch.flush();
            messageToTwitch.print("NICK " + Settings.getBotName() + "\r\n");
            messageToTwitch.flush();
            messageToTwitch.print("JOIN #" + Settings.getChannelName() + "\r\n");
            messageToTwitch.flush();
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + Settings.getHOST());
        } catch (IOException e) {
            System.err.println("Couldn't connect to " + Settings.getHOST());
        }
        try {
            boolean loading = false;
            String readBuffer = "";
            while (!loading) {
                readBuffer += messageFromTwitch.readLine();
                String[] lines = readBuffer.split("\n");
                for (String s : lines) {
                    //System.out.println(s);
                    loading = s.contains("End of /NAMES list");
                }
            }
            sendMessage("Successfully joined chat");
            System.out.println("Connected to chat");
        } catch (IOException e) {
            System.err.println("Couldn't connect to " + Settings.getHOST());
        }
    }

    /**
     * Start the bot running.
     * All parsing of twitch IRC is done here.
     */
    private static void startBot() {
        openSocket();
        Settings.setupSounds();
        checkMods();
        INNER_THREAD_TIMER_CLOCK.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                programRunning = false;
            }
        });
        while (programRunning) {
            String lineFromTwitch;
            try {
                while ((lineFromTwitch = messageFromTwitch.readLine()) != null) {
                    //Twitch pings the client every now and then, expecting a PONG reply.
                    if (lineFromTwitch.equals("PING :tmi.twitch.tv")) {
                        messageToTwitch.print("PONG :tmi.twitch.tv\r\n");
                        messageToTwitch.flush();
                        System.out.println("PONGED - (Responding to twitch's ping)");
                        checkMods();
                        continue;
                    }
                    //Get usable data from chat lines.
                    String parsedLineFromTwitch = lineFromTwitch.substring(lineFromTwitch.indexOf("#"));
                    String usernameOfSender = lineFromTwitch.substring(1, lineFromTwitch.indexOf("!"));
                    if (!Settings.getUsersCoolDown().containsKey(usernameOfSender)) {
                        Settings.getUsersCoolDown().put(usernameOfSender, -1000 - Settings.getUserCooldown());
                    }
                    String command;
                    try {
                        command = parsedLineFromTwitch.substring(parsedLineFromTwitch.indexOf("!")).replace(" ", "");
                    } catch (StringIndexOutOfBoundsException e) {
                        command = parsedLineFromTwitch.substring(parsedLineFromTwitch.indexOf(":"));
                    }
                    //Check for global updates
                    //Check for anything
                    if (command.toLowerCase().contains("!globalcooldown") && usernameOfSender.equalsIgnoreCase(Settings.getChannelName())) {
                        String cooldown = command.substring(15);
                        cooldown = cooldown.replace(" ", "");
                        try {
                            int newGlobalCooldown = Integer.parseInt(cooldown);
                            Settings.setGlobalCooldown(newGlobalCooldown);
                            sendMessage("Global Cooldown has been set to: " + newGlobalCooldown);
                        } catch (NumberFormatException e) {
                            System.out.println("WARNING - " + cooldown + " is not an acceptable number as the global cooldown!");
                        }
                    }


                    //Finally check for sounds.
                    Sound soundCommand = new Sound(command);
                    if (Settings.getSoundList().contains(soundCommand)) {
                       Sound soundToPlay = Settings.getSoundList().get(Settings.getSoundList().indexOf(soundCommand));
                        if (usernameOfSender.equalsIgnoreCase(Settings.getChannelName())) {
                            new SoundPlayer(soundToPlay);
                        } else if (mainTimer - soundToPlay.getLastUsed() >= Settings.getGlobalCooldown()) {
                            if ((Settings.getGlobalCooldown() == 0) || mainTimer - Settings.getSpecificUserCoolDown(usernameOfSender) >= Settings.getUserCooldown()) {
                                if (soundToPlay.getAccessType().equalsIgnoreCase("mod") && Settings.getModList().contains(usernameOfSender)) {
                                    new SoundPlayer(soundToPlay);
                                } else if (!soundToPlay.getAccessType().equalsIgnoreCase("mod")) {
                                    new SoundPlayer(soundToPlay);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("There were problems connecting to Twitch. Trying again.");
            }
        }
    }

    /**
     * Primary entry point.
     * @param args Launch arguments, primary use for adding, editing and deleting sounds.
     */
    public static void main(final String[] args) {
        if (args.length > 0) {
            //launch -a commandName location accessType
            //launch -e commandName location accessType
            //launch -d commandName
            switch (args[0]) {
                case "-a":
                    try {
                        //launch -a commandName location accessType
                        //args[0] = -a
                        //args[1] = commandName
                        //etc
                        if (new File(args[2]).exists()) {
                            Sound s = new Sound(args[1], args[2], args[3]);
                            Settings.addNewSound(s);
                        } else {
                            System.out.println("File not found!");
                        }
                        Thread.sleep(2000);

                    } catch (Exception e) {
                        System.err.println("Please use standard format:\nE.g: -a !commandTest \"C:\\...\\commandTest.mp3\" moderator 5\n");
                    }
                    break;
                case "-e":
                    if (new File(args[2]).exists()) {
                        Sound soundToEdit = new Sound(args[1]);
                        if (Settings.getSoundList().contains(soundToEdit)) {
                            soundToEdit.setFilePath(args[2]);
                            soundToEdit.setAccessType(args[3]);
                            Settings.editSound(soundToEdit);
                            System.out.println("Successfully edited: " + args[1]);
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            System.out.println("Problems editing sound.");
                        }
                    } else {
                        System.out.println("File not found!");
                    }
                    break;
                case "-d":
                    Sound soundToDelete = new Sound(args[1]);
                    if (Settings.getSoundList().contains(soundToDelete)) {
                        Settings.deleteSound(soundToDelete);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        System.out.println("Problems deleting sound.");
                    }
                    break;
                default:
                    Settings.setChannelName(args[0]);
                    Settings.setBotName(args[1]);
                    Settings.setAuthCode(args[2]);
                    startBot();
                    break;
            }
        }
        if (Settings.getChannelName() == null) {
            System.err.println("Please view the readme for setup!");
            System.exit(1);
        }
    }

    /**
     * Run a Twitch API call and then use JSON to parse
     * the moderators of the channel.
     */
    private static void checkMods() {
        try {
            URL chatList = new URL("http://tmi.twitch.tv/group/user/" + Settings.getChannelName() + "/chatters");
            try (InputStream is = chatList.openStream()) {
                JSONParser parser = new JSONParser();
                JSONObject obj = (JSONObject) parser.parse(new InputStreamReader(is));
                JSONObject chatter = (JSONObject) obj.get("chatters");
                JSONArray moderators = (JSONArray) chatter.get("moderators");
                //noinspection unchecked
                for (String currentMod : (Iterable<String>) moderators) {
                    if (!Settings.getModList().contains(currentMod)) {
                        Settings.addNewMod(currentMod);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read moderators from: "
                    + "http://tmi.twitch.tv/group/user/" + Settings.getChannelName() + "/chatters\n"
                    + "Check your internet connection.");
        } catch (org.json.simple.parser.ParseException e) {
            System.err.println("Failed to read JSON from "
                    + "http://tmi.twitch.tv/group/user/" + Settings.getChannelName() + "/chatters\n"
                    + "Check Twitch API Status!");
        }
    }

    /**
     * @return the current number of seconds since the program launch.
     */
    static int getMainTimer() {
        return mainTimer;
    }

}
