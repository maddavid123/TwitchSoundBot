package madd.TwitchBot;

/**
 * Blueprint for all Sounds.
 */
final class Sound {

    /**
     * The message sent in twitch chat to trigger the sound to be played.
     * E.g !blondie
     */
    private final String soundCommand;

    /**
     * The filepath (Directory and File) pointing towards the sound to be played.
     */
    private String filePath;

    /**
     * Who has access to the sound being played Chat Moderators or Normal users.
     */
    private String accessType;



    /**
     * The number of seconds since this sound was last used.
     * If it exceeds it's cooldown the sound can be played.
     * We're kinda cheating here, by using a negative number we ensure that it can be used at startup.
     */
    private int lastUsed = -5000;


    /**
     * General Constructor.
     * @param soundCommand  The message sent in twitch chat to trigger the sound to be played.
     * @param filePath      The filepath (Directory and File) pointing towards the sound to be played.
     * @param accessType    Who has access to the sound being played Chat Moderators or Normal users.
     */
    Sound(final String soundCommand, final String filePath, final String accessType) {
        this.soundCommand = soundCommand;
        this.filePath = filePath;
        this.accessType = accessType;
    }

    /**
     * A constructor made solely to compare a SoundCommand to an actual Sound Object.
     * @param soundCommand The message sent in twitch chat to trigger the sound to be played.
     */
    Sound(final String soundCommand) {
        this.soundCommand = soundCommand;
    }

    /**
     * @return The message sent in twitch chat to trigger the sound to be played.
     */
    String getSoundCommand() {
        return soundCommand;
    }


    /**
     * @return The filepath (Directory and File) pointing towards the sound to be played.
     */
    String getFilePath() {
        return filePath;
    }

    /**
     * @param filePath The filepath (Directory and File) pointing towards the sound to be played.
     */
    void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    /**
     * @return Who has access to the sound being played Chat Moderators or Normal users.
     */
    String getAccessType() {
        return accessType;
    }

    /**
     * @param accessType Set who has access to the sound being played Chat Moderators or Normal users.
     */
    void setAccessType(final String accessType) {
        this.accessType = accessType;
    }

    /**
     * @return The number of seconds since this sound was last used.
     */
    int getLastUsed() {
        return lastUsed;
    }

    /**
     * Normally called the moment a sound is used.
     * @param lastUsed Set the number of seconds since this sound was last used.
     */
    void setLastUsed(final int lastUsed) {
        this.lastUsed = lastUsed;
    }

    /**
     * Check whether the Sound incoming is the same as the one we have stored.
     * Since the Sound Command needs to be unique, we can test solely on this basis.
     * @param o the object we're comparing this Sound to.
     * @return whether or not the two sounds are the same.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Sound sound = (Sound) o;

        return getSoundCommand().equals(sound.getSoundCommand());

    }

    /**
     * @return the hashCode of the sound Command.
     */
    @Override
    public int hashCode() {
        return getSoundCommand().hashCode();
    }
}
