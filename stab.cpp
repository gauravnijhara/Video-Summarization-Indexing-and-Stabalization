#include <opencv2/opencv.hpp>
#include <iostream>
#include <cassert>
#include <cmath>
#include <fstream>

using namespace std;
using namespace cv;
//http://nghiaho.com/?p=2093
//prapthi:only those frames which have low motion shud be changed. and maybe the black border in the frames can be reduced by scaling the frames to fit the actual size
// This video stablisation smooths the global trajectory using a sliding average window

const int SMOOTHING_RADIUS = 50; // In frames. The larger the more stable the video, but less reactive to sudden panning
const int HORIZONTAL_BORDER_CROP = 20; // In pixels. Crops the border to reduce the black borders from stabilisation being too noticeable.

// 1. Get previous to current frame transformation (dx, dy, da) for all frames
// 2. Accumulate the transformations to get the image trajectory
// 3. Smooth out the trajectory using an averaging window
// 4. Generate new set of previous to current transform, such that the trajectory ends up being the same as the smoothed trajectory
// 5. Apply the new transformation to the video

struct TransformParam
{
    TransformParam() {}
    TransformParam(double _dx, double _dy, double _da) {
        dx = _dx;
        dy = _dy;
        da = _da;
    }

    double dx;
    double dy;
    double da; // angle
};

struct Trajectory
{
    Trajectory() {}
    Trajectory(double _x, double _y, double _a) {
        x = _x;
        y = _y;
        a = _a;
    }

    double x;
    double y;
    double a; // angle
};

int main(int argc, char **argv)
{


    // For further analysis
    ofstream out_transform("prev_to_cur_transformation.txt");
    ofstream out_trajectory("trajectory.txt");
    ofstream out_smoothed_trajectory("smoothed_trajectory.txt");
    ofstream out_new_transform("new_prev_to_cur_transformation.txt");
    ifstream file("/home/prapthi/Documents/prap/prapthi/prap/USC/Spring2016/project/Alin_Day1_002.zip.crdownload_FILES/Alin_Day1_002.rgb");
    int width = 480;
    int height = 270;

    Mat RGBFrame(270, 480, CV_8UC3);
    Mat RGBSecondFrame (270, 480, CV_8UC3);


    if(file.fail()) {
        cerr << "Error in reading file";
    }
    int len = 270*480*3;
    char bytes[len];
    char data[len];
    Mat RGBSecondFrame_grey;
    Mat RGBFrame_grey, last_T;
    int begin = len;
    int totalBytesRead = 0;
    // 1 - Get prev to cur frame transformation (dx, dy, da) for all frames
    vector <TransformParam> RGBFrame_to_RGBSecondFrame_transform;
    file.seekg(0, file.beg);

    for( int i = 0 ; i < 4500 ; i++)
			{
				// long lStartTime = System.currentTimeMillis();
				begin+= len;

				int offset = 0;
				int numRead = 0;

				if(i > 1)
				{
					RGBFrame.release();
					RGBSecondFrame.copyTo(RGBFrame);
				}

				RGBSecondFrame.release();
				RGBSecondFrame = Mat(270, 480, CV_8UC3);

//				if(i == 1480)
//				{
//					System.out.println("yaya here");
//				}

				/*while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
					offset += numRead;
				}*/

				file.seekg(begin, file.cur);
                file.read(bytes, len);

				int ind = 0;

				for(int y = 0; y < height; y++){

					for(int x = 0; x < width; x++){
						char a = 0;
						char r = bytes[ind];
						char g = bytes[ind+height*width];
						char b = bytes[ind+height*width*2];
                        //cout<<r<<" "<<g<<" "<<b;
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
					RGBSecondFrame = Mat(height,width,CV_8UC3,data);
					cvtColor(RGBSecondFrame, RGBSecondFrame_grey, COLOR_BGR2GRAY);

				}
				else
				{
					RGBFrame = Mat(height, width, CV_8UC3, data);

				}
				int row = RGBFrame.rows;
				int col = RGBFrame.cols;
				cout<<row<<" "<<col;
                cvtColor(RGBFrame, RGBFrame_grey, COLOR_BGR2GRAY);

                // vector from RGBFrame to RGBSecondFrame
                vector <Point2f> RGBFrame_corner, RGBSecondFrame_corner;
                vector <Point2f> RGBFrame_corner2, RGBSecondFrame_corner2;
                vector <uchar> status;
                vector <float> err;

                goodFeaturesToTrack(RGBFrame_grey, RGBFrame_corner, 5000, 0.2, 5);
                calcOpticalFlowPyrLK(RGBFrame_grey, RGBSecondFrame_grey, RGBFrame_corner, RGBSecondFrame_corner, status, err);

                // weed out bad matches
        for(size_t i=0; i < status.size(); i++) {
            if(status[i]) {
                RGBFrame_corner2.push_back(RGBFrame_corner[i]);
                RGBSecondFrame_corner2.push_back(RGBSecondFrame_corner[i]);
            }
        }

         // translation + rotation only
        Mat T = estimateRigidTransform(RGBFrame_corner2, RGBSecondFrame_corner2, false); // false = rigid transform, no scaling/shearing

        // in rare cases no transform is found. We'll just use the last known good transform.
        if(T.data == NULL) {
            last_T.copyTo(T);
        }

        T.copyTo(last_T);

        // decompose T
        double dx = T.at<double>(0,2);
        double dy = T.at<double>(1,2);
        double da = atan2(T.at<double>(1,0), T.at<double>(0,0));

        RGBFrame_to_RGBSecondFrame_transform.push_back(TransformParam(dx, dy, da));

        RGBSecondFrame.copyTo(RGBFrame);
        RGBSecondFrame_grey.copyTo(RGBFrame_grey);
	}

	// Step 2 - Accumulate the transformations to get the image trajectory

    // Accumulated frame to frame transform
    double a = 0;
    double x = 0;
    double y = 0;

    vector <Trajectory> trajectory; // trajectory at all frames

    for(size_t i=0; i < RGBFrame_to_RGBSecondFrame_transform.size(); i++) {
        x += RGBFrame_to_RGBSecondFrame_transform[i].dx;
        y += RGBFrame_to_RGBSecondFrame_transform[i].dy;
        a += RGBFrame_to_RGBSecondFrame_transform[i].da;

        trajectory.push_back(Trajectory(x,y,a));

        out_trajectory << (i+1) << " " << x << " " << y << " " << a << endl;
    }

    // Step 3 - Smooth out the trajectory using an averaging window
    vector <Trajectory> smoothed_trajectory; // trajectory at all frames

    for(size_t i=0; i < trajectory.size(); i++) {
        double sum_x = 0;
        double sum_y = 0;
        double sum_a = 0;
        int count = 0;

        for(int j=-SMOOTHING_RADIUS; j <= SMOOTHING_RADIUS; j++) {
            if(i+j >= 0 && i+j < trajectory.size()) {
                sum_x += trajectory[i+j].x;
                sum_y += trajectory[i+j].y;
                sum_a += trajectory[i+j].a;

                count++;
            }
        }

        double avg_a = sum_a / count;
        double avg_x = sum_x / count;
        double avg_y = sum_y / count;

        smoothed_trajectory.push_back(Trajectory(avg_x, avg_y, avg_a));

        out_smoothed_trajectory << (i+1) << " " << avg_x << " " << avg_y << " " << avg_a << endl;
    }

        // Step 4 - Generate new set of previous to current transform, such that the trajectory ends up being the same as the smoothed trajectory
    vector <TransformParam> new_RGBFrame_to_RGBSecondFrame_transform;

    // Accumulated frame to frame transform
    a = 0;
    x = 0;
    y = 0;

    for(size_t i=0; i < RGBFrame_to_RGBSecondFrame_transform.size(); i++) {
        x += RGBFrame_to_RGBSecondFrame_transform[i].dx;
        y += RGBFrame_to_RGBSecondFrame_transform[i].dy;
        a += RGBFrame_to_RGBSecondFrame_transform[i].da;

        // target - current
        double diff_x = smoothed_trajectory[i].x - x;
        double diff_y = smoothed_trajectory[i].y - y;
        double diff_a = smoothed_trajectory[i].a - a;

        double dx = RGBFrame_to_RGBSecondFrame_transform[i].dx + diff_x;
        double dy = RGBFrame_to_RGBSecondFrame_transform[i].dy + diff_y;
        double da = RGBFrame_to_RGBSecondFrame_transform[i].da + diff_a;

        new_RGBFrame_to_RGBSecondFrame_transform.push_back(TransformParam(dx, dy, da));

        //out_new_transform << (i+1) << " " << dx << " " << dy << " " << da << endl;
    }

    // Step 5 - Apply the new transformation to the video
    //cap.set(CV_CAP_PROP_POS_FRAMES, 0);
    Mat T(2,3,CV_64F);

    int vert_border = HORIZONTAL_BORDER_CROP * RGBFrame.rows / RGBFrame.cols; // get the aspect ratio correct


    for(int k=0; k<4499; k++) { // don't process the very last frame, no valid transform
        				int ind = 0;

				for(int y = 0; y < height; y++){

					for(int x = 0; x < width; x++){

						char a = 0;
						char r = bytes[ind];
						char g = bytes[ind+height*width];
						char b = bytes[ind+height*width*2];

		                data[ind * 3] = b;
		                data[ind * 3 + 1] = g;
		                data[ind * 3 + 2] = r;

						ind++;
						totalBytesRead += 3;
					}
				}

        RGBSecondFrame = Mat(0, 0, CV_8UC3, data);

        if(RGBSecondFrame.data == NULL) {
            break;
        }

        T.at<double>(0,0) = cos(new_RGBFrame_to_RGBSecondFrame_transform[k].da);
        T.at<double>(0,1) = -sin(new_RGBFrame_to_RGBSecondFrame_transform[k].da);
        T.at<double>(1,0) = sin(new_RGBFrame_to_RGBSecondFrame_transform[k].da);
        T.at<double>(1,1) = cos(new_RGBFrame_to_RGBSecondFrame_transform[k].da);

        T.at<double>(0,2) = new_RGBFrame_to_RGBSecondFrame_transform[k].dx;
        T.at<double>(1,2) = new_RGBFrame_to_RGBSecondFrame_transform[k].dy;

        Mat RGBSecondFrame2;

        warpAffine(RGBSecondFrame, RGBSecondFrame2, T, RGBSecondFrame.size());

        RGBSecondFrame2 = RGBSecondFrame2(Range(vert_border, RGBSecondFrame2.rows-vert_border), Range(HORIZONTAL_BORDER_CROP, RGBSecondFrame2.cols-HORIZONTAL_BORDER_CROP));

        // Resize RGBSecondFrame2 back to RGBSecondFrame size, for better side by side comparison
        resize(RGBSecondFrame2, RGBSecondFrame2, RGBSecondFrame.size());

        // Now draw the original and stablised side by side for coolness
        Mat canvas = Mat::zeros(RGBSecondFrame.rows, RGBSecondFrame.cols*2+10, RGBSecondFrame.type());

        RGBSecondFrame.copyTo(canvas(Range::all(), Range(0, RGBSecondFrame2.cols)));
        RGBSecondFrame2.copyTo(canvas(Range::all(), Range(RGBSecondFrame2.cols+10, RGBSecondFrame2.cols*2+10)));

        // If too big to fit on the screen, then scale it down by 2, hopefully it'll fit :)
        if(canvas.cols > 1920) {
            resize(canvas, canvas, Size(canvas.cols/2, canvas.rows/2));
        }

        imshow("before and after", canvas);

        //char str[256];
        //sprintf(str, "images/%08d.jpg", k);
        //imwrite(str, canvas);

        waitKey(20);
    }

    return 0;

}

