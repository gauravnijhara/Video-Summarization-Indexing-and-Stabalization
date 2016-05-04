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

    public static String videoFileNameFV;
    public static String audioFileNameFV;

    public String videoFileName;
    public String audioFileName;
    static boolean summarize = false;
    byte[] summarizedVidByte;

    //private static final String DEFAULT_LOCATION = "/home/prapthi/Documents/prap/prapthi/prap/USC/Spring2016/project/Alin_Day1_002.zip.crdownload_FILES";
    //private static final String DEFAULT_FILENAME = "Alin_Day1_002";
    //private static final String DEFAULT_MOVIE = DEFAULT_LOCATION + "/" + DEFAULT_FILENAME + ".rgb";
    //private static final String DEFAULT_SOUND = DEFAULT_LOCATION + "/" + DEFAULT_FILENAME + ".wav";

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
//for the entire video
    private void play() {
        for( int i = currentFrame; i < numberOfFrames; i++) {
            long lStartTime = System.currentTimeMillis();
            readFrame();
            try {
                long difference = (65) - (System.currentTimeMillis() - lStartTime);
                if(difference>0) {
                    sleep(difference);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
	timerThread.suspend();
    }
//for the entire video
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
//for the entire video
    private void createThreadsFV() {
        videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                play();
            }
        });

        audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playWAV(audioFileNameFV);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
    public void initialize() throws InterruptedException{
	createThreadsFV();

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try {
            File file = new File(videoFileNameFV);
            is = new FileInputStream(file);

            //long len = file.length();
            long len = width*height*3;
            numberOfFrames = file.length() / len;
            bytes = new byte[(int)len];

            int totalBytesRead = 0;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } /*catch (IOException e) {
            e.printStackTrace();
        }*/
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
		    long difference = (65) - (System.currentTimeMillis() - lStartTime);
		    if(difference>0) {
		        sleep(difference);
		    }
            }
	timerThread.suspend();
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
        //this.audioPerFrame = playSound.getSampleRate()/FRAME_RATE;
        // plays the sound
        try {
            playSound.play();
        } catch (PlayWaveException e) {
            e.printStackTrace();
            return;
        }
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
    public void summarize(String videoFileName, String audioFileName, ArrayList<Integer> input) throws InterruptedException {
	img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try {
	ren = this;
	list = new ArrayList<Integer>(input);
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
	File afile = new File(audioFileName);
            inputStream = new FileInputStream(afile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
	// initializes the playSound Object
	playSound = new PlaySound(inputStream);
	/*for (int i=1; i<150; i++) {
            list.add(i);
        }
        for (int i=450; i<600; i++) {
            list.add(i);
        }*/
       System.out.println("HERE!!"+list.size());
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
	AVPlayer ab = new AVPlayer();
/*        ren = new AVPlayer();
		//if (args.length < 2) {
            ren.audioFileName = DEFAULT_SOUND;
            ren.videoFileName = DEFAULT_MOVIE;
		/*} else {
            ren.audioFileName = args[1];
            ren.videoFileName = args[0];
        }*/
	//System.out.println(args[0]);
	if(args.length == 1 && args[0] == "zero")
	summarize = true;
	else if(args.length == 2) {
	audioFileNameFV = args[0];
        videoFileNameFV = args[1];
	}
	else {
		System.out.println("run as: java AVPlayer zero OR java AVPlayer audioFile videoFile");
	}
	//run it as: java AVPlayer zero
	ArrayList<Integer> input = new ArrayList<Integer>();
	for(int i=1; i<=150; i++) {
		input.add(i);	
	}
	for(int i=301; i<=450; i++) {
		input.add(i);	
	}

	if(summarize)
	ab.summarize("../Alin_Day1_002/Alin_Day1_002.rgb", "../Alin_Day1_002/Alin_Day1_002.wav", input);

        ab.setDisplay();
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
		    try {
		    if(summarize)
                    createThreads();
		    else
		    initialize();
                    status = PlayerStatus.PAUSED;
                    currentFrame = 0;
                    startThreads();
		    } catch (InterruptedException e1) {
                    e1.printStackTrace();
                    }
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
