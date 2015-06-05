package fr.xjet.pixaline;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hugo Gresse on 03/06/15.
 */
public class ImageRunnable implements Runnable {

    public static final String LOG_TAG = "ImageRunnable";

    private static int mJpegQuality = 50;
    private static int mWidthToCapture = 5 ;

    protected int                   mCameraWidth;
    protected int                   mCameraHeight;
    protected int                   mCameraPreviewFormat;

    protected int                   mKeepFrame;
    protected int                   mCurrentLine = 0;
    protected int                   mPixelLinePlace = 2; // Will take the pixel at width/2

    protected int                   mImageHeight;
    protected Bitmap                mFinalBitmap;
    protected BlockingQueue<byte[]> mImageBytesQueue;

    protected boolean               mIsRunning;

    protected Listener              mListener;

    @SuppressWarnings("deprecation")
    public ImageRunnable(Camera camera, byte[] bytes, ImageRunnable.Listener listener) {
        Log.d(LOG_TAG, "new ImageRunnable");
        mListener = listener;
        mImageBytesQueue = new LinkedBlockingQueue<>();


        Camera.Parameters parameters = camera.getParameters();
        mCameraWidth = parameters.getPreviewSize().width;
        mCameraHeight = parameters.getPreviewSize().height;
        mCameraPreviewFormat = parameters.getPreviewFormat();

        initializeFirstFrame(bytes);
    }

    private void initializeFirstFrame(byte[] bytes){
        YuvImage yuv = new YuvImage(bytes, mCameraPreviewFormat, mCameraWidth, mCameraHeight, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, mCameraWidth, mCameraHeight), mJpegQuality, out);

        byte[] bitmapsBytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapsBytes, 0, bitmapsBytes.length);

        if(bitmap == null){
            return;
        }

        mFinalBitmap = Bitmap.createBitmap(100, bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.recycle();
    }

    @Override
    public void run() {

        Log.d(LOG_TAG, "run");
        mIsRunning = true;

        Bitmap bitmap;

        while (mIsRunning) {
            try {
                byte[] data = mImageBytesQueue.take();

                Log.d(LOG_TAG, "process");
                // Convert byte array yuv to bitmap
                YuvImage yuv = new YuvImage(data, mCameraPreviewFormat, mCameraWidth, mCameraHeight, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, mCameraWidth, mCameraHeight), mJpegQuality, out);
                byte[] bitmapsBytes = out.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(bitmapsBytes, 0, bitmapsBytes.length);


                if(mCurrentLine == 0){
                    mPixelLinePlace = (int) (float)bitmap.getWidth() / mPixelLinePlace;
                }

                // Iterate on the pixel line
                for(int i = mCurrentLine; i < mCurrentLine + mWidthToCapture; i++){

                    Log.d(LOG_TAG, " - " + i);
                    for(int y =0; y < bitmap.getHeight(); y ++){
                        try {
                            mFinalBitmap.setPixel(i, y, bitmap.getPixel(mPixelLinePlace, y));

                        } catch (IllegalArgumentException e){
                            // Expand image
                            Bitmap tempBitmap = mFinalBitmap;
                            mFinalBitmap = Bitmap.createBitmap(tempBitmap.getWidth() + 100, bitmap.getHeight(), tempBitmap.getConfig());

                            Canvas tempCanvas = new Canvas(mFinalBitmap);
                            Rect src = new Rect(0, 0, tempBitmap.getWidth(), tempBitmap.getHeight());
                            tempCanvas.drawBitmap(tempBitmap, src, src, null);
                            i--;
                        }
                    }
                }

                mCurrentLine += mWidthToCapture;

                mListener.bitmapUpdated(mFinalBitmap);

                //handle the data
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, "Error occurred:", e);
            }
        }

    }

    public void stop() {
        mIsRunning = false;

    }

    public boolean isRunning(){
        return mIsRunning;
    }

    public void addByteArray(byte[] bytes){
        if(mKeepFrame <5){
            mKeepFrame ++;
            Log.d(LOG_TAG, "Skip this frame");
        } else {
            mKeepFrame = 0;
            Log.d(LOG_TAG, "addByteArray");
            mImageBytesQueue.add(bytes);
        }
    }

    public interface Listener {

        void bitmapUpdated(Bitmap bitmap);

    }
}
