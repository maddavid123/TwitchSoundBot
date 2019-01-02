package madd.TwitchBot;

import javazoom.jl.player.Player;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Play Sounds on their own thread.
 * WARNING: Large amounts of spam may cause lag.
 */
final class SoundPlayer extends Thread {

    /**
     * The filepath of the sound to be played.
      */
    private final String filepath;

    /**
     * The Constructor for this class.
     * @param s Sound to be played.
     */
    SoundPlayer(final Sound s) {
        if (s.getFilePath() != null) {
            filepath = s.getFilePath();
            start();
            s.setLastUsed(TwitchInterface.getMainTimer());
        } else {
            filepath = null;
        }
    }

    /**
     * Run a sound file using either Clip or javazoom's Player class.
     * Accepted file types: wav and mp3.
     */
    public void run() {
        File audioFile = new File(filepath);
        if (filepath.contains(".wav")) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                Clip audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.open(audioStream);
                audioClip.start();
                try {
                    Thread.sleep(audioClip.getMicrosecondLength() / 1000);
                } catch (InterruptedException e) {
                    System.out.println("Failed to play .wav file!");
                }
                audioClip.close();
                audioStream.close();

            } catch (IOException e) {
                System.err.println("Couldn't find audio file.");
            } catch (UnsupportedAudioFileException e) {
                System.err.println("This file type is unsupported.");
            } catch (LineUnavailableException e) {
                System.err.println("This output line is unavailable.");
            }
        } else if (filepath.contains(".mp3")) {
            try {
                FileInputStream fis = new FileInputStream(filepath);
                Player playerMP3 = new Player(fis);
                playerMP3.play();
            } catch (FileNotFoundException e) {
                System.err.println("File was not found!\nPlease ensure that \"" + filepath + "\" exists!");
                //System.exit(0);
            } catch (javazoom.jl.decoder.JavaLayerException e) {
                System.err.println("File could not be decoded");
                //System.exit(0);
            }
        }
    }
}
