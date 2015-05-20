package fr.xjet.pixaline;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import fr.xjet.pixaline.utils.VideoHelper;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Read the given video path to extract each frame to create a new iamge with one pixel line of each video frame.
 *
 * Created by Hugo Gresse on 20/05/15.
 */
public class CalculateFinalImageTask extends AsyncTask<String, Void, String> {

    public static final String LOG_TAG = "CalculateFinalImageTask";

    protected final int mFrameInterval = 50; //Take one image very 50ms
    protected final int mPixelLinePlace = 2; // Will take the pixel at width/2

    protected Context  mContext;
    protected Listener mListener;
    protected int      mCapturedFrame = 0;


    public CalculateFinalImageTask(Context context, Listener listener) {
        super();
        mContext = context;
        mListener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {

        String videoPath = strings[0];

        if(videoPath == null) {
            return null;
        }

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");



        // Get Video Duration to know how much picture we will get and so how large will be output image.
        FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
        mmr.setDataSource(videoPath);
        String duration = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        Log.d(LOG_TAG, "" + duration);

        int videoDuration = Integer.parseInt(duration);
        int numberOfFrameToGet = videoDuration / mFrameInterval;


        VideoHelper.saveCurrentPicture(
                mContext,
                mmr.getFrameAtTime(0, FFmpegMediaMetadataRetriever.OPTION_CLOSEST),
                sdf.format(new Date()) + "-first.jpg");


        Bitmap currentBitmap = mmr.getFrameAtTime(0, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

        int pixelLinePlace = (int) (float)currentBitmap.getWidth() / mPixelLinePlace;
        int imageHeight = currentBitmap.getHeight();
        int imageWidth = currentBitmap.getWidth();

        Bitmap outputBitmap = Bitmap.createBitmap(numberOfFrameToGet, imageHeight, Bitmap.Config.ARGB_8888);

        Log.d(LOG_TAG, "numberOfFrameToGet:" + numberOfFrameToGet + " \n pixelLinePlace" + pixelLinePlace + " \n" );

        // Iterate on video frame
        for(int x =0; x < numberOfFrameToGet; x++){

            Log.d(LOG_TAG, "Processing line " + x + " out of " + numberOfFrameToGet);

            currentBitmap = mmr.getFrameAtTime(x * mFrameInterval * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

            // Iterate on the pixel line
            for(int y =0; y < imageHeight; y ++){
                outputBitmap.setPixel(x, y, currentBitmap.getPixel(pixelLinePlace, y));
            }

        }


        return VideoHelper.saveCurrentPicture(
                mContext,
                outputBitmap,
                sdf.format(new Date()) + ".jpg");
    }

    @Override
    protected void onPostExecute(String path) {
        Log.d(LOG_TAG, "CalculateFinalImageTask onPostExecute " + path);
        if(mListener != null){
            mListener.onFinish(path);
        }
    }



    public interface Listener  {
        void onFinish(String path);
    }


}
