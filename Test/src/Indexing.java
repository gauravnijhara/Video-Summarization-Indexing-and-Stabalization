
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

public class Indexing {

	public static String videoFileNameFV = "../Alireza_Day2_003/Alireza_Day2_003.rgb";
	public static String audioFileNameFV = "../Alireza_Day2_003/Alireza_Day2_003.wav"; 
	public static String metaFileNameFV = "Alireza_Day2_003.metadata";
	public static String imgFileNameFV = "../images/Alireza_Day2_003/29952.rgb";

	int iwidth = 1280;
	int iheight = 720;

	static FeatureDetector featureDet;
	static DescriptorExtractor descExtract;
	static DescriptorMatcher matcher;
	static Mat indexDescriptor;
	static Integer maxMatchCount;
	static ObjectInputStream ois;
    static ArrayList<Double> distanceArray = new ArrayList<Double>();

	
	// setup display
	static JFrame frame;
	static JLabel lbIm1;
	static JLabel lbIm2;
	static BufferedImage img;
	static BufferedImage indeximg;

	public void calculateIndexedImageFeatures(String index) throws IOException
	{
		try
		{
			
	        indeximg = new BufferedImage(iwidth, iheight, BufferedImage.TYPE_INT_RGB);
			featureDet = FeatureDetector.create(FeatureDetector.ORB);
			descExtract = DescriptorExtractor.create(DescriptorExtractor.ORB);
			matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
			indexDescriptor = new Mat();

			
	
			File file = new File(index);
			InputStream is = new FileInputStream(file);
			

			//long len = file.length();
			long len = iwidth*iheight*3;
			byte[] bytes = new byte[(int)len];
			byte[] data = new byte[(int)len];
			Mat frame = new Mat(iheight, iwidth, CvType.CV_8UC3);
			Mat yuvframe = new Mat();
			
			int totalBytesRead = 0;
	
			int offset = 0;
			int numRead = 0;
			
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}
			
			int ind = 0;
	
			for(int y = 0; y < iheight; y++){
	
				for(int x = 0; x < iwidth; x++){
	
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+iheight*iwidth];
					byte b = bytes[ind+iheight*iwidth*2]; 
	
	                data[ind * 3] = b;
	                data[ind * 3 + 1] = g;
	                data[ind * 3 + 2] = r;
	
					ind++;
					
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					indeximg.setRGB(x,y,pix);
				}
			}
	
		//	lbIm1.setIcon(new ImageIcon(indeximg));


			frame.put(0, 0, data);
			Imgproc.resize(frame,yuvframe,new Size(480,270));

			
			MatOfDMatch matches = new MatOfDMatch();
			MatOfKeyPoint features = new MatOfKeyPoint();
			featureDet.detect(yuvframe, features);
			descExtract.compute(yuvframe , features, indexDescriptor);
			matcher.match(indexDescriptor,indexDescriptor, matches);
			maxMatchCount = matches.toArray().length;
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
			
	}

	public final Mat loadMat(ObjectInputStream ois) {
	    try {
	    	
	    	
	        int cols;
            cols = (int) ois.readObject();
	        if(cols > 0)
	        {
		        byte[] data;
		        data = (byte[]) ois.readObject();
		        Mat mat = new Mat(data.length / cols, cols, CvType.CV_8UC1);
		        mat.put(0, 0, data);
		        return mat;
	        }
	        else
	        {
	        	return null;
	        }
	    } catch (IOException | ClassNotFoundException | ClassCastException ex) {
	        System.err.println("ERROR: Could not load mat from file: ");
	    }
	    return null;
	}
	
	
	public final int returnIndex()
	{
		for(int j = 0 ; j < 4500 ; j++)
		{
			Mat descriptor = loadMat(ois);
			if(null != descriptor)
			{
				ArrayList<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
				matcher.radiusMatch(descriptor,indexDescriptor,matches,180);
				double sum = 0;		
				
				for(int i=0 ; i < matches.size() ; i++)
				{			
					if(matches.get(i).toArray().length > 0)
						sum++;
				}
				
				distanceArray.add(sum);
			}
			else
			{
				distanceArray.add((double) 0);
			}
		}
		
		int frameIndexMatch = 0;
		for(int di =1 ; di < distanceArray.size() ; di++)
		{
			if(distanceArray.get(di) > distanceArray.get(frameIndexMatch))
			{
				frameIndexMatch = di;
			}
		}
		
 		return frameIndexMatch;
		
	}
	
	public static void displayFrame(String filename, int frameNum) throws IOException
	{
		setDisplay();
		
		int width = 480;
		int height = 270;
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		long skipNum = frameNum*width*height*3;
		File file = new File(filename);
		InputStream is = new FileInputStream(file);
		is.skip(skipNum);

		//long len = file.length();
		long len = width*height*3;
		byte[] bytes = new byte[(int)len];
		byte[] data = new byte[(int)len];
		Mat frame = new Mat(height, width, CvType.CV_8UC3);
		Mat yuvframe = new Mat();
		
		int totalBytesRead = 0;

		int offset = 0;
		int numRead = 0;
		
		while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
			offset += numRead;
		}
		
		int ind = 0;

		for(int y = 0; y < height; y++){

			for(int x = 0; x < width; x++){

				byte a = 0;
				byte r = bytes[ind];
				byte g = bytes[ind+height*width];
				byte b = bytes[ind+height*width*2]; 

				ind++;
				
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				img.setRGB(x,y,pix);
			}
		}


	}
	
	public static void setDisplay()
	{
		// Use labels to display the images
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Index");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Video frame");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		lbIm1 = new JLabel();
		lbIm2 = new JLabel();


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
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);


	}

	public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
//		if (args.length < 2) {
//		    System.err.println("usage: java -jar AVPlayer.jar [RGB file] [WAV file]");
//		    return;
//		}
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//String audioFile = args[0];
		//String videoFile = args[1];
		//String metadata = args[2];
		//String image = args[3];
		//String[] extract = audioFile.split("/");
		/*String tempStr = extract[extract.length-1];
		metadata+= tempStr.substring(0, tempStr.length()-4);
		metadata+=".metadata";*/
		Indexing temp = new Indexing();
		ois = new ObjectInputStream(new FileInputStream(metaFileNameFV));
		temp.calculateIndexedImageFeatures(imgFileNameFV);
		
		int frame = temp.returnIndex();
		System.out.println("best frame match is " + frame);
		
		int remX = (int)(frame%15);
		int nomalizedIndex = frame - remX - 15;
		
		ArrayList<Integer> summaryInput = new ArrayList<Integer>();
		
		for(int i = nomalizedIndex + 1 ; i < nomalizedIndex + 75 ; i++)
		{
			summaryInput.add(i);
		}
		
		AVPlayer player = new AVPlayer();
		player.summarize(videoFileNameFV,audioFileNameFV, summaryInput);
		player.setDisplay();
		
		ois.close();

	}
	
}
