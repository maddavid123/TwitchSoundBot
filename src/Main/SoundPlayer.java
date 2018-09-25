package Main;

import javazoom.jl.player.Player;
import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by David on 19/03/2018.
 */
public class SoundPlayer extends Thread {
    String filepath;
    int volume;
    public SoundPlayer(Sound s){
        filepath = s.getFilePath();

        start();
        s.setLastUsed(Connection.mainTimer);
    }


    public void run() {
        File audioFile = new File(filepath);
        if(filepath.contains(".wav")) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                Clip audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.open(audioStream);
                FloatControl volumeFloat = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                volumeFloat.setValue(-1 * volume);
                audioClip.start();
                try {
                    Thread.sleep(audioClip.getMicrosecondLength()/1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        }
        else if(filepath.contains(".mp3")){
            try {
                FileInputStream fis = new FileInputStream(filepath);
                Player playerMP3 = new Player(fis);
                playerMP3.play();
            } catch (FileNotFoundException e) {
                System.err.println("File was not Found");
                System.exit(0);
            } catch(javazoom.jl.decoder.JavaLayerException e){
                System.err.println("File could not be decoded");
                System.exit(0);
            }
        }
    }
}
