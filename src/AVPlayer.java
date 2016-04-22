
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import static java.lang.Thread.sleep;


public class AVPlayer {

    JFrame frame;
    JLabel lbIm1;
    JLabel lbIm2;
    BufferedImage img;

    private static final String DEFAULT_LOCATION = "/Users/ayberk/Downloads/Alin_Day1_002";
    private static final String DEFAULT_FILENAME = "Alin_Day1_002";
    private static final String DEFAULT_MOVIE = DEFAULT_LOCATION + "/" + DEFAULT_FILENAME + ".rgb";
    private static final String DEFAULT_SOUND = DEFAULT_LOCATION + "/" + DEFAULT_FILENAME + ".wav";

    private static final int FRAME_RATE = 15;
    private static final int LENGTH = 5; // in minutes [todo] this should not be hardcoded

    float audioPerFrame = 1;
    PlaySound playSound;
    int width = 480;
    int height = 270;
    InputStream is;
    byte[] bytes;

    public void initialize() throws InterruptedException{

        setDisplay();
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try {
            File file = new File(DEFAULT_MOVIE);
            is = new FileInputStream(file);

            //long len = file.length();
            long len = width*height*3;
            bytes = new byte[(int)len];

            int totalBytesRead = 0;


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void play() {
        for( int i = 0 ; i < LENGTH*60*FRAME_RATE; i++) {
            readFrame();
            try {
                sleep(1000 / FRAME_RATE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void readFrame() {
        int offset = 0;
        int numRead = 0;

        try {
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }
            lbIm1.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void playVideo() throws InterruptedException {
        initialize();
        Thread videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                    play();
            }
        });

        Thread soundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playWAV(DEFAULT_SOUND);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        videoThread.start();
        soundThread.start();
    }

    public void playWAV(String filename) throws InterruptedException {
        // opens the inputStream
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // initializes the playSound Object
        playSound = new PlaySound(inputStream);
        this.audioPerFrame = playSound.getSampleRate()/FRAME_RATE;
        // plays the sound
        try {
            playSound.play();
            sleep(10000);
        } catch (PlayWaveException e) {
            e.printStackTrace();
            return;
        }
    }

    public void setDisplay()
    {
        // Use labels to display the images
        frame = new JFrame();
        frame.setSize(600, 400);
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        //JLabel lbText1 = new JLabel("Video: " + args[0]);
        JLabel lbText1 = new JLabel("Video: ");
        lbText1.setHorizontalAlignment(SwingConstants.LEFT);
        //JLabel lbText2 = new JLabel("Audio: " + args[1]);
        JLabel lbText2 = new JLabel("Audio: ");
        lbText2.setHorizontalAlignment(SwingConstants.LEFT);
        lbIm1 = new JLabel(new ImageIcon());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        frame.getContentPane().add(lbText1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbText2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        frame.getContentPane().add(lbIm1, c);

        frame.setVisible(true);
    }

    public static void main(String[] args) throws InterruptedException {
//		if (args.length < 2) {
//		    System.err.println("usage: java -jar AVPlayer.jar [RGB file] [WAV file]");
//		    return;
//		}
        AVPlayer ren = new AVPlayer();
        ren.playVideo();
    }

}