package madd.TwitchBot;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility Class for all the primarily used variables and database connectivity.
 */
final class Settings {

    /**
     * The website we're connecting to.
     */
    private static final String HOST = "irc.twitch.tv";

    /**
     * The port of the HOST we're connecting to.
     */
    private static final int PORT = 6667;

    /**
     * The password (OAuth code) we're using to connect to Twitch with.
     */
    private static String authCode;

    /**
     * The Twitch username of the account we're using.
     */
    private static String botName;

    /**
     * The Twitch username of the channel we're connecting to.
     */
    private static String channelName;

    /**
     * An ArrayList containing all of the sounds used by the bot.
     */
    private static final ArrayList<Sound> SOUND_LIST = new ArrayList<>();

    /**
     * An ArrayList containing all of the usernames of moderators of the channel we're connecting to.
     */
    private static final ArrayList<String> MOD_LIST = new ArrayList<>();

    /**
     * A hashmap tracking every user in chat, and the last time they used a sound command.
     */
    private static final HashMap<String, Integer> USERS_COOL_DOWN = new HashMap<>();

    /**
     * Stores whether or not the database exists.
     */
    private static boolean dbExists = false;

    /**
     * Stores what version of the database is in use.
     */
    private static String dbVersion;

    /**
     * Stores the current cooldown time in seconds for all sound commands.
     */
    private static int globalCooldown;

    /**
     * Sets the number of seconds before a single user can use another sound command.
     * Separate from Global Cooldown which is the number of seconds before the same sound can be used again.
     */
    private static int userCooldown;

    /**
     * Not used.
     */
    private Settings() {
    }

    /**
     * Check for the existence of a config file.
     */
     private static void startupCFGValidation() {
        File configFile = new File("config.cfg");
        boolean cfgExists = configFile.exists();
        if (!cfgExists) {
            try {
                boolean succeededInCreatingFile = configFile.createNewFile();
                if (succeededInCreatingFile) {
                    PrintWriter writer = new PrintWriter(new FileWriter("config.cfg", true));
                    writer.write("globalCooldownDefault=30\n");
                    writer.flush();
                    writer.write("userCooldownDefault=" + 30 + "\n");
                    writer.flush();
                }
            } catch (IOException e) {
                System.out.println("Failed to create CFG file!");
                System.exit(0);
            }
        } else {
            try {
                BufferedReader br = new BufferedReader(new FileReader("config.cfg"));
                String lineBeingRead;
                while ((lineBeingRead = br.readLine()) != null) {
                    if (lineBeingRead.contains("globalCooldownDefault=")) {
                        //Remove all spaces, and take the numbers after equals sign
                        String readCooldown = (lineBeingRead.substring(lineBeingRead.indexOf("=") + 1)).replaceAll(" ", "");
                        try {
                            globalCooldown = Integer.parseInt(readCooldown);
                        } catch (NumberFormatException e) {
                            globalCooldown = 60;
                            System.err.println("Malformed Config file. Please look at config.cfg");
                        }
                    } else if (lineBeingRead.contains("userCoolDownDefault")) {
                        String readCooldown = (lineBeingRead.substring(lineBeingRead.indexOf("=") + 1)).replaceAll(" ", "");
                        try {
                            userCooldown = Integer.parseInt(readCooldown);
                        } catch (NumberFormatException e) {
                            userCooldown = 30;
                            System.err.println("Malformed Config file. Please look at config.cfg");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Failed to open config.cfg file for reading");
            }
        }

    }

    /**
     * Check for Database existence and version.
     */
    private static void startupDBValidation() {
        Connection databaseConnection = null;
        dbExists = new File("database.db").exists();
        if (dbExists) {
            try {
                Class.forName("org.sqlite.JDBC");
                databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
                Statement dbCommand  = databaseConnection.createStatement();
                String sql = "SELECT * FROM Settings";
                dbCommand.executeQuery(sql);
                dbVersion = "1.0";
                dbCommand.close();

            } catch (SQLException e) {
                dbVersion = "2.0";
            } catch (ClassNotFoundException e) {
                System.out.println("Could not access database.");
            } finally {
                if (databaseConnection != null) {
                    try {
                        databaseConnection.close();
                    } catch (SQLException e) {
                        System.out.println("Could not access database.");
                    }
                }
            }
        } else {
            createNewDatabase();
        }
        if (dbVersion.equals("1.0")) {
            migrateDatabase();
        }
    }

    /**
     * Create an SQLite database and tables.
     */
    private static void createNewDatabase() {
        Connection databaseConnection;
        try {
            Class.forName("org.sqlite.JDBC");
            databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement dbCommand  = databaseConnection.createStatement();
            String sql = "CREATE TABLE Sound(\n"
                    + "soundCommand   STRING  PRIMARY KEY,\n"
                    + "filepath       STRING  NOT NULL,\n"
                    + "accessType     STRING  NOT NULL\n"
                    + ");";
            dbCommand.execute(sql);
            sql = "CREATE TABLE Mod(Username   STRING  PRIMARY KEY);";
            dbCommand.execute(sql);
            dbCommand.close();
            databaseConnection.close();
            dbVersion = "2.0";
        } catch (SQLException e) {
            System.out.println("Malformed SQL code, Tell David he fucked up");
        } catch (ClassNotFoundException e) {
            System.out.println("Failed to connect to database");
        }
    }

    /**
     * Update database to the current version.
     */
    private static void migrateDatabase() {
        Connection databaseConnection;
        ArrayList<String> oldModerators = new ArrayList<>();
        ArrayList<Sound> oldSounds      = new ArrayList<>();
        try {
            Class.forName("org.sqlite.JDBC");
            databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement dbCommand  = databaseConnection.createStatement();
            String sql = "SELECT * FROM Mod";
            ResultSet rs = dbCommand.executeQuery(sql);
            while (rs.next()) {
                oldModerators.add(rs.getString("Username"));
            }
            sql = "SELECT * FROM SOUND";
            rs = dbCommand.executeQuery(sql);
            while (rs.next()) {
                globalCooldown      = rs.getInt("cooldown");
                String soundCommand = rs.getString("soundCommand");
                String filePath     = rs.getString("filepath");
                String accessType   = rs.getString("accessType");
                oldSounds.add(new Sound(soundCommand, filePath, accessType));
            }
            dbCommand.close();
            databaseConnection.close();
        } catch (SQLException e) {
            System.out.println("Malformed SQL code, Tell David he fucked up");
        } catch (ClassNotFoundException e) {
            System.out.println("Failed to connect to database");
        }
        File oldDatabase = new File("database.db");
        //noinspection ResultOfMethodCallIgnored
        oldDatabase.delete();
        createNewDatabase();
        oldModerators.forEach(Settings::addNewMod);
        oldSounds.forEach(Settings::addNewSound);
    }

    /**
     * Iterate through a database file and add every sound to SOUND_LIST.
     */
    static void setupSounds() {
        startupDBValidation();
        startupCFGValidation();
        MOD_LIST.add(getChannelName());
        if (dbExists) {
            Connection databaseConnection;
            try {
                Class.forName("org.sqlite.JDBC");
                databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
                Statement dbCommand  = databaseConnection.createStatement();
                String sql = "SELECT * FROM Mod";
                ResultSet rs = dbCommand.executeQuery(sql);
                while (rs.next()) {
                    MOD_LIST.add(rs.getString("Username"));
                }
                sql = "SELECT * FROM SOUND";
                rs = dbCommand.executeQuery(sql);
                while (rs.next()) {
                    String soundCommand = rs.getString("soundCommand");
                    String filePath     = rs.getString("filepath");
                    String accessType   = rs.getString("accessType");
                    SOUND_LIST.add(new Sound(soundCommand, filePath, accessType));
                }
                dbCommand.close();
                databaseConnection.close();
            } catch (SQLException e) {
                System.out.println("Malformed SQL code, Tell David he fucked up");
            } catch (ClassNotFoundException e) {
                System.out.println("Failed to connect to database");
            }
        }

    }

    /**
     * Connect to a database file and add a new sound to it.
     * @param soundToAdd The new sound to add to the database.
     */
    static void addNewSound(final Sound soundToAdd) {
        startupDBValidation();
        if (dbExists) {
            Connection databaseConnection;
            try {
                Class.forName("org.sqlite.JDBC");
                databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement dbCommand  = databaseConnection.prepareStatement("INSERT INTO Sound\nVALUES(?,?,?);");
                dbCommand.setString(1, soundToAdd.getSoundCommand());
                dbCommand.setString(2, soundToAdd.getFilePath());
                dbCommand.setString(3, soundToAdd.getAccessType());
                dbCommand.execute();
                System.out.println("Successfully added: " + soundToAdd.getSoundCommand());
                dbCommand.close();
                databaseConnection.close();
            } catch (SQLException e) {
                System.out.println(soundToAdd.getSoundCommand() + " may already exist! Please delete and try again.");
            } catch (ClassNotFoundException e) {
                System.out.println("Failed to connect to database");
            }
        }
    }

    /**
     * Connect to a database and edit a sound in it.
     * @param soundToEdit The sound to edit.
     */
    static void editSound(final Sound soundToEdit) {
        startupDBValidation();
        if (dbExists) {
            Connection databaseConnection;
            try {
                Class.forName("org.sqlite.JDBC");
                databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement dbCommand = databaseConnection.prepareStatement("UPDATE Sound\n"
                        + "SET soundCommand=?,filepath=?,accessType=?\n"
                        + "WHERE soundCommand=?");
                dbCommand.setString(1, soundToEdit.getSoundCommand());
                dbCommand.setString(2, soundToEdit.getFilePath());
                dbCommand.setString(3, soundToEdit.getAccessType());
                dbCommand.setString(4, soundToEdit.getSoundCommand());
                dbCommand.execute();
                dbCommand.close();
            } catch (SQLException e) {
                System.out.println("Malformed SQL code, Tell David he fucked up");
            } catch (ClassNotFoundException e) {
                System.out.println("Failed to connect to database");
            }
        }
    }

    /**
     * Connect to a database and delete a sound in it.
     * @param soundToDelete The sound to delete.
     */
    static void deleteSound(final Sound soundToDelete) {
        startupDBValidation();
        if (dbExists) {
            Connection databaseConnection;
            try {
                Class.forName("org.sqlite.JDBC");
                databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement dbCommand = databaseConnection.prepareStatement("DELETE FROM SOUND\n"
                        + "WHERE soundCommand=?");
                dbCommand.setString(1, soundToDelete.getSoundCommand());
                dbCommand.execute();
                dbCommand.close();
            } catch (SQLException e) {
                System.out.println("Malformed SQL code, Tell David he fucked up");
            } catch (ClassNotFoundException e) {
                System.out.println("Failed to connect to database");
            }
        }
    }

    /**
     * Connect to a database and add a new moderator to it.
     * @param modName The username of the moderator we're adding.
     */
    static void addNewMod(final String modName) {
        startupDBValidation();
        if (dbExists) {
            Connection databaseConnection;
            try {
                Class.forName("org.sqlite.JDBC");
                databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement dbCommand = databaseConnection.prepareStatement("INSERT INTO MOD\nVALUES(?);");
                dbCommand.setString(1, modName);
                dbCommand.execute();
                MOD_LIST.add(modName);
                dbCommand.close();
                databaseConnection.close();
            } catch (SQLException e) {
                System.out.println("Malformed SQL code, Tell David he fucked up");
            } catch (ClassNotFoundException e) {
                System.out.println("Failed to connect to database");
            }
        }
    }

    /**
     * @return the host/website we're connecting to.
     */
    static String getHOST() {
        return HOST;
    }

    /**
     * @return the port of the host we're connecting on.
     */
    static int getPORT() {
        return PORT;
    }

    /**
     * @return the OAuth code (password) of the twitch account we're using.
     */
    static String getAuthCode() {
        return authCode;
    }

    /**
     * @return the name of the twitch account we're using.
     */
    static String getBotName() {
        return botName;
    }

    /**
     * @return the name of the twitch channel we're connecting to.
     */
    static String getChannelName() {
        return channelName;
    }

    /**
     * @return the ArrayList of Sounds to be used by chatters.
     */
    static ArrayList<Sound> getSoundList() {
        return SOUND_LIST;
    }

    /**
     * @return the ArrayList of Moderators of the channel we're connected to.
     */
    static ArrayList<String> getModList() {
        return MOD_LIST;
    }

    /**
     * @return the time (in seconds) since a specific user last used a sound command.
     * @param username the specific user we're searching for the cooldown for
     */
    static int getSpecificUserCoolDown(final String username) {
        return USERS_COOL_DOWN.get(username);
    }

    /**
     * @return the HashMap containing all users and their cooldowns in seconds.
     */
    static HashMap<String, Integer> getUsersCoolDown() {
        return USERS_COOL_DOWN;
    }

    /**
     * @param authCode Set the password (OAuth) of the twitch account we're using.
     */
    static void setAuthCode(final String authCode) {
        Settings.authCode = authCode;
    }

    /**
     * @param botName Set the username of the twitch account we're using.
     */
    static void setBotName(final String botName) {
        Settings.botName = botName;
    }

    /**
     * @param channelName Set the username of the twitch channel we're connecting to.
     */
    static void setChannelName(final String channelName) {
        Settings.channelName = channelName;
    }

    /**
     * @return The current cooldown time in seconds for all sound commands.
     */
    static int getGlobalCooldown() {
        return globalCooldown;
    }

    /**
     * @param globalCooldown Set the current cooldown time in seconds for all sound commands.
     */
    static void setGlobalCooldown(final int globalCooldown) {
        Settings.globalCooldown = globalCooldown;
    }

    /**
     * @return the number of seconds before a single user can play another sound command.
     */
    public static int getUserCooldown() {
        return userCooldown;
    }
}
