package fr.xjet.pixaline;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import java.text.SimpleDateFormat;
import java.util.Date;

import fr.xjet.pixaline.utils.VideoHelper;

/**
 * Read the given video path to extract each frame to create a new iamge with one pixel line of each video frame.
 *
 * Created by Hugo Gresse on 20/05/15.
 */
public class CalculateFinalImageTask extends AsyncTask<String, Void, String> {

    public static final String LOG_TAG = "CalculateFinalImageTask";

    protected Context mContext;
    protected Listener mListener;


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

        return VideoHelper.saveCurrentPicture(
                mContext,
                VideoHelper.getVideoFrame(videoPath),
                sdf.format(new Date()) + ".jpg");
    }

    @Override
    protected void onPostExecute(String path) {
        if(mListener != null){
            mListener.onFinish(path);
        }
    }



    public interface Listener  {
        void onFinish(String path);
    }


}
