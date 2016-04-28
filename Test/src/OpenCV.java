import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.opencv.core.*;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.*;


public class OpenCV {
	
    static ArrayList<Mat> metaData = new ArrayList<Mat>();
    static ArrayList<Mat> YUVImages = new ArrayList<Mat>();
    static ArrayList<Integer> SADValues = new ArrayList<Integer>();
    static ArrayList<Double> distanceArray = new ArrayList<Double>();
	static FeatureDetector featureDet;
	static DescriptorExtractor descExtract;
	static DescriptorMatcher matcher;
	static Mat indexDescriptor;
	static Integer maxMatchCount;

	// setup display
	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage img,indeximg;

	public void initOpevCV( String[] args ) 
	   {
		
		setDisplay();
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		featureDet = FeatureDetector.create(FeatureDetector.ORB);
		descExtract = DescriptorExtractor.create(DescriptorExtractor.ORB);
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
		indexDescriptor = new Mat();
				
		int width = 480;
		int height = 270;

		boolean isSecondFrameDecoded = false;
		Mat RGBframe = new Mat(270, 480, CvType.CV_8UC3);
		Mat RGBSecondframe = new Mat(270, 480, CvType.CV_8UC3);
        
        
        ArrayList<Integer> frameIndices = new ArrayList<Integer>();
        

        indeximg = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		try {
			
			calculateIndexedImageFeatures();

			File file = new File("../Alireza_Day2_003/Alireza_Day2_003.rgb");
			InputStream is = new FileInputStream(file);
			//is.skip(1244160000);
			
			
			//long len = file.length();
			long len = width*height*3;
			byte[] bytes = new byte[(int)len];
			byte[] data = new byte[(int)len];

			int totalBytesRead = 0;
			
			
			for( int i = 0 ; i < 4500 ; i++)
			{
				// long lStartTime = System.currentTimeMillis();
				
				int offset = 0;
				int numRead = 0;
				
				if(i > 1)
				{
					RGBframe.release();
					RGBframe = RGBSecondframe.clone();
				}
				
				RGBSecondframe.release();
				RGBSecondframe = new Mat(270, 480, CvType.CV_8UC3);
				
//				if(i == 1480)
//				{
//					System.out.println("yaya here");
//				}
				
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
	
		                data[ind * 3] = b;
		                data[ind * 3 + 1] = g;
		                data[ind * 3 + 2] = r;
						
		                
//		                int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
//						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
//						img.setRGB(x,y,pix);
						
						ind++;
						totalBytesRead += 3;
						

					}
				}
				
				if(i > 0)
				{
					//RGBframe.release();
					RGBSecondframe.put(0, 0, data);
					calculateMetadata(RGBSecondframe);
						//lbIm2.setIcon(new ImageIcon(img));

					//performOpticalAnalysis(RGBframe,RGBSecondframe);
					
				}
				else
				{
					RGBframe.put(0, 0, data);
					//calculateMetadata(RGBframe);

				}
				
				
		
			}

			
			// get extremas for video histogram
			// uncomment for summarization
			/* 
			 ArrayList<Integer> mins = new ArrayList<Integer>();
			 ArrayList<Point> intervals = new ArrayList<Point>();
			 
			 mins.add(0);
			 
			 int index = 0;
			 int prevValue = SADValues.get(index++);
			 int value = SADValues.get(index);
			 
			 double avg = 0.0;
			 for(int s = 0 ; s < 4499 ; s++)
			 {
				 avg += SADValues.get(s);
			 }
			 
			 avg /= 4499;
			 avg *= 2.80;
			 
			 if(avg > 6000)
				 avg = 6000;

			 while(index+1 < SADValues.size())
			 {
				 if(prevValue > value)
				 {
					 int startValue = prevValue;
					 prevValue = value;
					 value = SADValues.get(++index);
					 
					 while(index+1 < SADValues.size() && prevValue > value)
					 {
						 index++;
						 prevValue = value;
						 value = SADValues.get(index);

					 }
					 
					 if(Math.abs(startValue - prevValue) > avg)
						 mins.add(index-1);
				 }
				 else
				 {
					 int startValue = prevValue;
					 prevValue = value;
					 value = SADValues.get(++index);
					 
					 while(index+1 < SADValues.size() && prevValue < value)
					 {
						
						 index++;
						 prevValue = value;
						 value = SADValues.get(index);
					 }
					 
					 if(Math.abs(startValue - prevValue) > avg)
						 mins.add(index-1);
				 }
			 }
			 
			 mins.add(4499);
			 
			 int totalFrameNum = 0;
			 //process range
			 int j = 0;
			 while(j < mins.size())
			 {
				 int frameCount = 0;
				 Point temp = new Point();
				 temp.x = mins.get(j);
				 while(j+1 < mins.size() && (( mins.get(j+1) - mins.get(j) ) < 150 ))
				 {
					 j++;
					 frameCount++;
				 }
				 
				 if(frameCount == 0)
				 {
					if(temp.x == 4499)
					{
						temp.x -= 45;
						temp.y = 4499;
					}
					else
					{
						temp.y = temp.x + 45;
					}
				 }
				 else
				 {
					 temp.y = mins.get(j);
					 if((4499 - temp.y) < 60)
					 {
						temp.y = 4499;
						j = mins.size();
					 }
				 }
				 
				 // round off to seconds boundaries
				 int remX = (int)(temp.x%15);
				 temp.x -= remX;
				 temp.x = (temp.x != 0)?temp.x-1:temp.x;
				 
				 if(temp.y != 4499)
				 {
					 int remY = (int)(temp.y%15);
					 temp.y += (15-remY);
					 temp.y--;
				 }
				 
				 if((temp.y - temp.x) < 60)
				 {
					 int diff = (int) (60 - (temp.y - temp.x));
					 temp.y += diff;
				 }
				 
				 totalFrameNum += temp.y - temp.x;
				 intervals.add(temp);
				 
				 j++;
			 }
			  
		*/
			
		// match features
			int frameIndexMatch = 0;
			for(int di =1 ; di < distanceArray.size() ; di++)
			{
				if(distanceArray.get(di) > distanceArray.get(frameIndexMatch))
				{
					frameIndexMatch = di;
				}
			}
			
			System.out.println("best frame match is " + frameIndexMatch);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	    
	   
	   }
	
	public static void performOpticalAnalysis(Mat oldFrame, Mat newFrame)
	{
			Mat YUVframeOld = new Mat();
			Mat YUVframeNew = new Mat();
	        MatOfByte status = new MatOfByte();
	        MatOfFloat error = new MatOfFloat();

			MatOfPoint corners_old = new MatOfPoint();
			MatOfPoint corners_new = new MatOfPoint();
			MatOfPoint2f corners_old2f = new MatOfPoint2f();
			MatOfPoint2f corners_new2f = new MatOfPoint2f();
			
			
			Imgproc.cvtColor(oldFrame,YUVframeOld,Imgproc.COLOR_BGR2GRAY,0);
			Imgproc.goodFeaturesToTrack(YUVframeOld, corners_old, 5000, 0.2, 5);
		    corners_old.convertTo(corners_old2f, CvType.CV_32FC2);
		        
		    Imgproc.cvtColor(newFrame,YUVframeNew,Imgproc.COLOR_BGR2GRAY,0);
		    Imgproc.goodFeaturesToTrack(YUVframeNew, corners_new,5000, 0.2, 2);			        
	        corners_new.convertTo(corners_new2f, CvType.CV_32FC2);
	        
			YUVImages.add(YUVframeNew);
			
	        
		   // Video.calcOpticalFlowPyrLK(YUVframeOld,YUVframeNew,corners_old2f, corners_new2f, status, error);
		    Video.calcOpticalFlowPyrLK(YUVframeOld,YUVframeNew,corners_old2f, corners_new2f, status, error,new Size(240.0,135.0),5);
		    // calculate SAD
		    float[] errFound = error.toArray();
		    
		    int it = 0,sadVal = 0;
		    while(it < errFound.length)
		    {
//		    	if(i > 1600)
//				 {
//					System.out.println("yaya here");
//				 }
		    	
//		    	if((statusFound[it] == 1))
//		    	{
//		    		sadVal += (int) Math.abs(newPoints[it].x - oldPoints[it].x) + (int) Math.abs(newPoints[it].y - oldPoints[it].y);
//		    	}
//		    	else
//		    	{
//		    		int xOffset = (int) (width/2 - oldPoints[it].x);
//		    		xOffset = xOffset<0?((width/2 + xOffset)):xOffset;
//		    		int yOffset = (int) (height/2 - oldPoints[it].y);
//		    		yOffset = yOffset<0?((height/2 + yOffset)):yOffset;
//
//		    		sadVal += (xOffset + yOffset);
//		    	}
		    	sadVal += errFound[it];
		    	
		    	it++;
		    }
		    
		    SADValues.add(sadVal);
		    
		    corners_old.release();		    
		    corners_old2f.release();    
		    YUVframeOld.release();
   		    YUVframeNew.release();
		    corners_new.release();
		    corners_new2f.release();
	        status.release();
	        error.release();

	}
	
	public void calculateMetadata(Mat a)
	{
		
		Mat yuvFrame = new Mat();
		Mat descriptor = new Mat();
		MatOfKeyPoint features = new MatOfKeyPoint();
		//MatOfDMatch matches = new MatOfDMatch();
		ArrayList<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
		Imgproc.cvtColor(a,yuvFrame,Imgproc.COLOR_BGR2GRAY,0);

		
		featureDet.detect(a, features);
		descExtract.compute(a, features, descriptor);
		matcher.radiusMatch(descriptor,indexDescriptor,matches,180);
		//DMatch[] matchArr = matches.toArray();
		double sum = 0;
		
		double minDist = 100000,maxDist = 0;
		
//		for(int i=0 ; i < matchArr.length ; i++)
//		{
//			if(matchArr[i].distance < minDist)
//				minDist = matchArr[i].distance;
//			
//			if(matchArr[i].distance > maxDist)
//				maxDist = matchArr[i].distance;
//
//		}
//		
//
		for(int i=0 ; i < matches.size() ; i++)
		{
//			if(matchArr[i].distance < 2*minDist)
//				sum++;
			
			if(matches.get(i).toArray().length > 0)
				sum++;

		}

		//double normalizedValue = matchArr.length + (1/sum)*100000;
		distanceArray.add(sum);
		

		//metaData.add(descriptor);
	}
	
	public void calculateIndexedImageFeatures() throws IOException
	{
		try
		{
			int width = 1280;
			int height = 720;
	
			File file = new File("../images/Alireza_Day2_003/30598.rgb");
			InputStream is = new FileInputStream(file);
			

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
	
	                data[ind * 3] = b;
	                data[ind * 3 + 1] = g;
	                data[ind * 3 + 2] = r;
	
					ind++;
					
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					indeximg.setRGB(x,y,pix);
				}
			}
	
			lbIm1.setIcon(new ImageIcon(indeximg));


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
	
	public void setDisplay()
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
	public static void main(String[] args) throws InterruptedException {
//		if (args.length < 2) {
//		    System.err.println("usage: java -jar AVPlayer.jar [RGB file] [WAV file]");
//		    return;
//		}
		OpenCV test = new OpenCV();
		test.initOpevCV(args);
	}
	}
