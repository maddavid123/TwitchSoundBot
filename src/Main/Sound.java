package Main;

/**
 * Created by David on 14/03/2018.
 */
public class Sound {
    private String soundCommand;
    private String filePath;
    private String accessType;
    private int cooldown;
    private int defaultCooldown;
    private int lastUsed;

    public Sound(String soundCommand, String filePath, String accessType, int cooldown) {
        this.soundCommand = soundCommand;
        this.filePath = filePath;
        this.accessType = accessType;
        this.cooldown = cooldown;
        defaultCooldown = cooldown;
        lastUsed = -5000;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public String getSoundCommand() {
        return soundCommand;
    }

    public void setSoundCommand(String soundCommand) {
        this.soundCommand = soundCommand;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public int getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(int lastUsed) {
        this.lastUsed = lastUsed;
    }

    public int getDefaultCooldown() {
        return defaultCooldown;
    }
}
