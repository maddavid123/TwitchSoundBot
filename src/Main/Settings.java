package Main;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by David on 14/03/2018.
 */
public class Settings {
    public static String HOST = "irc.twitch.tv";
    public static int PORT = 6667;
    public static String PASS; //Remember to snip
    public static String NICK; //Remember to snip
    public static String CHANNEL; //Remember to snip
    public static ArrayList<Sound> soundList = new ArrayList<>();
    public static ArrayList<String> modsList = new ArrayList<>();
    public static int perUserCoolDown = 30; //Seconds
    public static int defaultUserCoolDown = 30;
    public static HashMap<String, Integer> usersCooldown = new HashMap<>();
    private static boolean dbExist = false;

    public static void setupSounds(){
        dbExist = testDB();
        if(!dbExist){
            java.sql.Connection c = null;
            try{
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:database.db");
                Statement s  = c.createStatement();
                String sql = "CREATE TABLE Sound(\n" +
                        "soundCommand   STRING  PRIMARY KEY,\n" +
                        "filepath       STRING  NOT NULL,\n" +
                        "accessType     STRING  NOT NULL,\n" +
                        "cooldown       INT     NOT NULL\n" +
                        ");";
                s.execute(sql);
                sql = "CREATE TABLE Mod(Username   String  PRIMARY KEY);";
                s.execute(sql);
                sql = "CREATE TABLE Settings(SettingID  INT PRIMARY KEY,\n" +
                        "perUserCoolDown INT);";
                s.execute(sql);
                sql = "INSERT INTO Settings\nVALUES(1,30)";
                s.execute(sql);
                s.close();
                dbExist = true;
            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                System.err.println("Don't know about that class.");
            }
            finally {
                try{
                    if(c != null){
                        c.close();
                    }
                }catch(SQLException e){
                    System.err.println("Couldn't connect to database");
                }
            }
        }
        if(dbExist){
            java.sql.Connection c = null;
            try{
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:database.db");
                Statement s  = c.createStatement();
                String sql = "SELECT *  FROM Sound";
                ResultSet rs = s.executeQuery(sql);
                while(rs.next()){
                    soundList.add(new Sound(rs.getString("soundCommand"),rs.getString("filepath"),rs.getString("accessType"), rs.getInt("cooldown")));
                }
                sql = "SELECT * FROM Mod";
                rs = s.executeQuery(sql);
                while(rs.next()){
                    modsList.add(rs.getString("Username"));
                }
                sql = "Select * From Settings";
                rs = s.executeQuery(sql);
                while(rs.next()){
                    defaultUserCoolDown = rs.getInt("perUserCoolDown");
                }
                perUserCoolDown = defaultUserCoolDown;
                s.close();

            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                System.err.println("Don't know about that class.");
            }
            finally {
                try{
                    if(c != null){
                        c.close();
                    }
                }catch(SQLException e){
                    System.err.println("Couldn't connect to database");
                }
            }
        }
        modsList.add(CHANNEL);
    }
    public static void setPerUserCoolDown(int i){
        if(dbExist){

            java.sql.Connection c = null;
            try{
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement stmn = c.prepareStatement("UPDATE SETTINGS\n" +
                        "SET perUserCoolDown = ?;");
                stmn.setInt(1,i);
                stmn.execute();
                stmn.close();
            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                System.err.println("Don't know about that class.");
            }finally {
                try{
                    if(c != null){
                        c.close();
                    }
                }catch(SQLException e){
                    System.err.println("Couldn't connect to database");
                }
            }
        }
    }
    public static void addNewSound(Sound s){
        if(dbExist){
            soundList.add(s);
            java.sql.Connection c = null;
            try{
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement stmn = c.prepareStatement("INSERT INTO Sound\n" +
                        "VALUES(?,?,?,?);");
                stmn.setString(1,s.getSoundCommand());
                stmn.setString(2,s.getFilePath());
                stmn.setString(3,s.getAccessType());
                stmn.setInt(4,s.getDefaultCooldown());
                stmn.execute();
                stmn.close();
            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                System.err.println("Don't know about that class.");
            }
            finally {
                try{
                    if(c != null){
                        c.close();
                    }
                }catch(SQLException e){
                    System.err.println("Couldn't connect to database");
                }
            }
        }
    }
    public static void addNewMod(String s){
        if(dbExist){
            modsList.add(s);
            java.sql.Connection c = null;
            try{
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement stmn = c.prepareStatement("INSERT INTO Mod\n" +
                        "VALUES(?);");
                stmn.setString(1,s);
                stmn.execute();
                stmn.close();
            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                System.err.println("Don't know about that class.");
            }finally {
                try{
                    if(c != null){
                        c.close();
                    }
                }catch(SQLException e){
                    System.err.println("Couldn't connect to database");
                }
            }
        }
    }
    public static void editSound(Sound s){
        if(dbExist){
            for(int i = 0; i<soundList.size();i++){
                Sound savedSound = soundList.get(i);
                if(savedSound.getSoundCommand().equals(s.getSoundCommand())){
                    soundList.remove(i);
                    soundList.add(s);
                }
            }
            java.sql.Connection c = null;
            try{
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement stmn = c.prepareStatement("UPDATE Sound\n" +
                        "SET soundCommand=?,filepath=?,accessType=?,cooldown=?\n" +
                        "WHERE soundCommand=?");
                stmn.setString(1,s.getSoundCommand());
                stmn.setString(2,s.getFilePath());
                stmn.setString(3,s.getAccessType());
                stmn.setFloat(4,s.getDefaultCooldown());
                stmn.setString(5,s.getSoundCommand());
                stmn.execute();
                stmn.close();
            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                System.err.println("Couldn't bridge connection to database.");
            }
            finally {
                try{
                    if(c != null){
                        c.close();
                    }
                }catch(SQLException e){
                    System.err.println("Couldn't connect to database");
                }
            }
        }
    }
    public static void deleteSound(String soundCommand){
        if(dbExist){
            for(int i = 0; i<soundList.size();i++){
                Sound savedSound = soundList.get(i);
                if(savedSound.getSoundCommand().equals(soundCommand)){
                    soundList.remove(i);
                }
            }
            java.sql.Connection c = null;
            try{
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement stmn = c.prepareStatement("DELETE FROM Sound\n" +
                        "WHERE soundCommand=?");
                stmn.setString(1,soundCommand);
                stmn.execute();
                stmn.close();
            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                System.err.println("Don't know about that class.");
            }
            finally {
                try{
                    if(c != null){
                        c.close();
                    }
                }catch(SQLException e){
                    System.err.println("Couldn't connect to database");
                }
            }
        }
    }
    public static boolean testDB(){
        java.sql.Connection c = null;
        boolean tableExist = true;
        try{
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement s  = c.createStatement();
            String sql = "INSERT INTO Sound\n" +
                    "VALUES('doubleTest','test','Admin','50');";
            s.execute(sql);
            sql = "DELETE FROM Sound\n" +
                    "WHERE filepath = 'test'";
            s.execute(sql);
            s.close();
        } catch(SQLException e){
            tableExist = false;
            System.err.println("Malformed SQL");
        } catch(ClassNotFoundException e){
            System.err.println("Don't know about that class.");
        }
        finally {
            try{
                if(c != null){
                    c.close();
                }
            }catch(SQLException e){
                System.err.println("Couldn't connect to database");
            }
        }
        return tableExist;
    }


}
