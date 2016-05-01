import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;

public class AVPlayer implements ActionListener {

    PlayerStatus status = PlayerStatus.STOPPED;
    JFrame frame;
    JLabel lbIm1;
    JLabel lbIm2;
    JLabel lbText1;
    BufferedImage img;
    JButton playPauseButton;
    JButton stopButton;

    Thread videoThread;
    Thread audioThread;
    Thread timerThread;

    public String videoFileName;
    public String audioFileName;
    static boolean summarize = false;
    byte[] summarizedVidByte;

    private static final String DEFAULT_LOCATION = "/Users/ayberk/Downloads/Alin_Day1_002";
    private static final String DEFAULT_FILENAME = "Alin_Day1_002";
    private static final String DEFAULT_MOVIE = DEFAULT_LOCATION + "/" + DEFAULT_FILENAME + ".rgb";
    private static final String DEFAULT_SOUND = DEFAULT_LOCATION + "/" + DEFAULT_FILENAME + ".wav";

    private static final int FRAME_RATE = 15;

    private long numberOfFrames = 0;
    //float audioPerFrame = 1;
    PlaySound playSound;
    static AVPlayer ren;
    int width = 480;
    int height = 270;
    InputStream is;
    byte[] bytes;
    int currentFrame = 0;
    List<Integer> list;

    private void createThreads() {

	videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                ren.playSpecificFrames(list);
		} catch (InterruptedException E) {
		E.printStackTrace();
		}
            }
        });
	audioThread = new Thread(new Runnable() {
            @Override
            public void run() {

	try {
	    playSound.playSpecificFrames();
	} catch (PlayWaveException e) {
	    e.printStackTrace();
	    return;
	} catch (InterruptedException e1) {
	    e1.printStackTrace();
	}
    		}
        });

	timerThread = new Thread(new Runnable() {
            int counter = 0;
            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    counter++;
                    lbText1.setText(counter + " seconds");
                }
            }
        });
    }

public void videoInitialize(List<Integer> frames) 
{
	int currentFrame = 0, frameIndex = 0;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	int  offset = 0, numRead = 0, len =width*height*3;
	try 
	{
		while (currentFrame<numberOfFrames && frameIndex < frames.size())  
		{
			offset = 0; numRead = 0;
			//sumSize+=size;
			while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) 
			{
		        	offset += numRead;
			}
			if (currentFrame == frames.get(frameIndex))
			{
				out.write(bytes, 0, bytes.length);
				bytes = new byte[(int)len];
				frameIndex++;
			}
			currentFrame++;
		}
	} 
	catch (Exception e) 
	{
            e.printStackTrace();
        }
	summarizedVidByte = out.toByteArray();

}
    public void playSpecificFrames(List<Integer> frames) throws InterruptedException {
       
int ind=0;
            // show these
            for(int i=0; i<frames.size(); i++)  {
		    long lStartTime = System.currentTimeMillis();
		    readFrameSum(ind);
		    ind+=width*height*3;
		    long difference = (1000/FRAME_RATE) - (System.currentTimeMillis() - lStartTime);
		    if(difference>0) {
		        sleep(difference);
		    }
            }
    }
public void readFrameSum(int ind) {
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {

            byte a = 0;
            byte r = summarizedVidByte[ind];
            byte g = summarizedVidByte[ind + height * width];
            byte b = summarizedVidByte[ind + height * width * 2];

            int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                img.setRGB(x, y, pix);
            ind++;
        }
    }lbIm1.setIcon(new ImageIcon(img));
}

    public void setDisplay()
    {
        // Use labels to display the images
        frame = new JFrame();
        frame.setSize(650, 400);
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        //JLabel lbText1 = new JLabel("Video: " + args[0]);
        lbText1 = new JLabel("Video: ");
        lbText1.setHorizontalAlignment(SwingConstants.LEFT);
        //JLabel lbText2 = new JLabel("Audio: " + args[1]);
        JLabel lbText2 = new JLabel("Audio: ");
        lbText2.setHorizontalAlignment(SwingConstants.LEFT);
        lbIm1 = new JLabel(new ImageIcon());
        lbIm1.setSize(width, height);

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

        c.gridx = 0;
        c.gridy = 3;
        playPauseButton = new JButton();
        playPauseButton.setText("Play");
        playPauseButton.addActionListener(this);
        frame.getContentPane().add(playPauseButton, c);

        c.gridx = 1;
        c.gridy = 3;
        stopButton = new JButton();
        stopButton.setText("Stop");
        stopButton.addActionListener(this);
        frame.getContentPane().add(stopButton, c);

        frame.setVisible(true);
    }
    public void summarize() throws InterruptedException {
	img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try {
            File file = new File(videoFileName);
            is = new FileInputStream(file);

            //long len = file.length();
            long len = width*height*3;
            numberOfFrames = file.length() / len;
            bytes = new byte[(int)len];

            int totalBytesRead = 0;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

	FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(audioFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
	// initializes the playSound Object
	playSound = new PlaySound(inputStream);
        playSound.fileName = audioFileName;
	list = new ArrayList<Integer>();
        for (int i=4350; i<4500; i++) {
            list.add(i);
        }

	try {
	    playSound.audioInitialize(list);
	    ren.videoInitialize(list);
	} catch (PlayWaveException e) {
	    e.printStackTrace();
	    return;
	}
	
	//videoInitialize(list);
    }
    public static void main(String[] args) throws InterruptedException, PlayWaveException {
        ren = new AVPlayer();
		//if (args.length < 2) {
            ren.audioFileName = DEFAULT_SOUND;
            ren.videoFileName = DEFAULT_MOVIE;
		/*} else {
            ren.audioFileName = args[1];
            ren.videoFileName = args[0];
        }*/
	//System.out.println(args[0]);
	//run it as: java AVPlayer zero

        try {
            System.out.println(PlaySound.getDominantFrequency(DEFAULT_SOUND));
        } catch (Exception e) {
            e.printStackTrace();
        }

//	ren.summarize();

//        ren.setDisplay();
	/*if(summarize) {
        System.out.println("here in main, IF block");
	
        List<Integer> list = new ArrayList<>();
        for (int i=50; i<100; i++) {
            list.add(i);
        }
        ren.playSummarize(list);
	}*/
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playPauseButton) {
            if (status == PlayerStatus.STOPPED) {
                    createThreads();
                    status = PlayerStatus.PAUSED;
                    currentFrame = 0;
                    startThreads();
               

            }
            if (status == PlayerStatus.PAUSED) {
                status = PlayerStatus.PLAYING;
                playPauseButton.setText("Pause");
                resumeThreads();
            } else {
                status = PlayerStatus.PAUSED;
                playPauseButton.setText("Play");
                suspendThreads();
            }
        }

        if (e.getSource() == stopButton) {
            status = PlayerStatus.STOPPED;
            playPauseButton.setText("Play");
            suspendThreads();
        }
    }

    private void suspendThreads() {
        videoThread.suspend();
        audioThread.suspend();
        timerThread.suspend();
    }

    private void startThreads() {
        videoThread.start();
        audioThread.start();
        timerThread.start();
    }

    private void resumeThreads() {
        videoThread.resume();
        audioThread.resume();
        timerThread.resume();
    }
}
