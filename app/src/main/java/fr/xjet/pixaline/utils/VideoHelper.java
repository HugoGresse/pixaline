package fr.xjet.pixaline.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Video utils
 *
 * Created by Hugo Gresse on 20/05/15.
 */
public class VideoHelper {

    public static final String LOG_TAG = "VideoHelper";

    public static Bitmap getVideoFrame(String path) {
        // TODO : use https://github.com/wseemann/FFmpegMediaMetadataRetriever
        Log.d(LOG_TAG, "getVideoFrame from " + path);

        FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
        mmr.setDataSource(path);

        Bitmap b = mmr.getFrameAtTime(500000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST); // frame at 500ms

        mmr.release();

        return b;
    }


    /**
     * Save given bitmap to external temp dir
     * @param context app context
     * @param bitmap bitmap to save
     * @param outputPath file name
     * @return the image path, null if not saved
     */
    public static String saveCurrentPicture(Context context, @Nullable Bitmap bitmap, String outputPath){
        try {
            if (bitmap == null) {
                return null;
            }

            File imageFile = new File(context.getExternalCacheDir(), outputPath);

            if(!imageFile.exists()){

                // Convert to InputStream;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                // TODO : add option to change output quality
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                byte[] bitmapdata = bos.toByteArray();

                FileOutputStream output = new FileOutputStream(imageFile);
                InputStream inputStream = new ByteArrayInputStream(bitmapdata);
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
                inputStream.close();
                output.close();
                return imageFile.toString();
            } else {
                Log.d(LOG_TAG, "File Already exist");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
