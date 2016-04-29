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

	static FeatureDetector featureDet;
	static DescriptorExtractor descExtract;
	static DescriptorMatcher matcher;
	static Mat indexDescriptor;
	static Integer maxMatchCount;
	static ObjectInputStream ois;
    static ArrayList<Double> distanceArray = new ArrayList<Double>();

	
	// setup display
	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage img,indeximg;

	public void calculateIndexedImageFeatures(String index) throws IOException
	{
		try
		{
			
	        indeximg = new BufferedImage(1280, 960, BufferedImage.TYPE_INT_RGB);
			featureDet = FeatureDetector.create(FeatureDetector.ORB);
			descExtract = DescriptorExtractor.create(DescriptorExtractor.ORB);
			matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
			indexDescriptor = new Mat();

			
			int width = 1280;
			int height = 960;
	
			File file = new File(index);
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
	
			//lbIm1.setIcon(new ImageIcon(indeximg));


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
	public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
//		if (args.length < 2) {
//		    System.err.println("usage: java -jar AVPlayer.jar [RGB file] [WAV file]");
//		    return;
//		}
		
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		Indexing temp = new Indexing();
		ois = new ObjectInputStream(new FileInputStream("Yin_Snack.metadata"));
		temp.calculateIndexedImageFeatures("../images/Yin_Snack/6472.rgb");
		int frame = temp.returnIndex();
		System.out.println("best frame match is " + frame);
		ois.close();

	}
	
}
