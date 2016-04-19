import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.swing.ImageIcon;

import org.opencv.core.*;
import org.opencv.core.Size;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.*;


public class OpenCV {
	
    static ArrayList<MatOfPoint2f> metaData = new ArrayList<MatOfPoint2f>();
    static ArrayList<Integer> SADValues = new ArrayList<Integer>();

	public static void main( String[] args )
	   {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		int width = 480;
		int height = 270;

		boolean isSecondFrameDecoded = false;
		Mat RGBframe = new Mat(270, 480, CvType.CV_8UC3);
		Mat RGBSecondframe = new Mat(270, 480, CvType.CV_8UC3);
        

    	BufferedImage img;

        
        ArrayList<Integer> frameIndices = new ArrayList<Integer>();
        


		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		try {
			
			
			File file = new File("../Alireza_Day2_003/Alireza_Day2_003.rgb");
			InputStream is = new FileInputStream(file);
			//is.skip(575424000);
			
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
						
						ind++;
						totalBytesRead += 3;
					}
				}
				
				if(i > 0)
				{
					//RGBframe.release();
					RGBSecondframe.put(0, 0, data);
					performOpticalAnalysis(RGBframe,RGBSecondframe);
					
				}
				else
				{
					RGBframe.put(0, 0, data);
				}
				
				
		
			}

			
			// get extremas for video histogram
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
			 
			System.out.println("yaya here");
			
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
	
	}
