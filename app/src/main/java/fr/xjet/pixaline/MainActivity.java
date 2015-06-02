package fr.xjet.pixaline;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.xjet.pixaline.utils.CameraHelper;
import fr.xjet.pixaline.utils.VideoHelper;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    public static final String LOG_TAG = "MainActivity";

    protected Camera        mCamera;

    protected TextureView   mTextureView;

    protected Camera.PictureCallback mPictureCallback;

    protected MediaRecorder mMediaRecorder;
    protected String        mCurrentImageName;
    protected File          mOutputFile;

    protected CalculateFinalImageTask mCalculateImageTask;
    protected Bitmap                  mOutputImage;

//    private Extractor extractor;


    protected boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        mCurrentImageName = sdf.format(new Date());

        mTextureView = (TextureView) findViewById(R.id.previewView);
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.previewView)
    public void toggleRecord() {
        if (mIsRecording) {
            mIsRecording = false;
            Log.d(LOG_TAG, "Stop recording : " + mOutputFile.toString());
        } else {
            mIsRecording = true;
        }
    }

    private boolean prepareCamera(boolean preview){
        Log.d(LOG_TAG, "prepateCamera");

        // BEGIN_INCLUDE (configure_preview)
        if(mCamera == null){
            mCamera = CameraHelper.getDefaultCameraInstance();
        }

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        mCamera.lock();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                mTextureView.getWidth(), mTextureView.getHeight());

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        CameraHelper.setCameraDisplayOrientation(this, mCamera);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());

            if(preview){
                mCamera.startPreview();
                mCamera.setPreviewCallback(this);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        // END_INCLUDE (configure_preview)

        return true;
    }

    private boolean prepareVideoRecorder(){
        Log.d(LOG_TAG, "prepareVideoRecorder");

        if(mCamera == null){
            prepareCamera(false);
        } else {
            try {
                mCamera.reconnect();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                mTextureView.getWidth(), mTextureView.getHeight());

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        Log.d(LOG_TAG, "Profil recorder width:"+profile.videoFrameWidth + " height:" +profile.videoFrameHeight);

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }

        mCamera.stopPreview();

        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);

        // Step 4: Set output file
        try {
            mOutputFile = CameraHelper.getTempOutputMediaFile(
                    this,
                    CameraHelper.MEDIA_TYPE_VIDEO);
            mMediaRecorder.setOutputFile(mOutputFile.toString());
        } catch (NullPointerException e){
            e.printStackTrace();
            return false;
            // report with fabric
        }
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(LOG_TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            // release the camera for other applications
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }


    private void processVideoToImage(){

        Toast.makeText(
                MainActivity.this,
                getString(R.string.msg_image_processing_start),
                Toast.LENGTH_SHORT).show();

        mCalculateImageTask = new CalculateFinalImageTask(this, new CalculateFinalImageTask.Listener() {
            @Override
            public void onFinish(String path) {
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.msg_image_processing_done) +  " " + path,
                        Toast.LENGTH_SHORT).show();


                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + path), "image/*");
                startActivity(intent);


            }
        });

        mCalculateImageTask.execute(mOutputFile.toString());
    }


    /*********************************************
     * Implement TextureView.SurfaceTextureListener
     *********************************************/

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        if(!mIsRecording){
            prepareCamera(true);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.d(LOG_TAG, "onSurfaceTextureDestroyed");
        releaseCamera();
        releaseMediaRecorder();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }


    /*********************************************
     * Implement Camera.PreviewCallback
     *********************************************/

    private int mCurrentLine = 0;
    protected int mPixelLinePlace = 2; // Will take the pixel at width/2
    private int mMaxLine = 200;
    private static int mJpegQuality = 50;
    private int mCameraWidth;
    private int mCameraHeight;
    private int mCameraPreviewFormat;

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if(mOutputImage == null){
            initializeOutputImage(camera, bytes);
        }

        if(!mIsRecording){
            return;
        }

        updateOutputImage(camera, bytes);
    }

    protected void initializeOutputImage(Camera camera, byte[] bytes){
        Log.d(LOG_TAG, "initializeOutputImage bytes:" + bytes.length);


        Camera.Parameters parameters = camera.getParameters();
        mCameraWidth = parameters.getPreviewSize().width;
        mCameraHeight = parameters.getPreviewSize().height;

        YuvImage yuv = new YuvImage(bytes, mCameraPreviewFormat = parameters.getPreviewFormat(), mCameraWidth, mCameraHeight, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, mCameraWidth, mCameraHeight), mJpegQuality, out);

        byte[] bitmapsBytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapsBytes, 0, bitmapsBytes.length);

        if(bitmap == null){
            return;
        }

        mOutputImage = Bitmap.createBitmap(mMaxLine, bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.recycle();
    }

    protected void updateOutputImage(Camera camera, byte[] bytes){

        if(mCurrentLine >= mMaxLine){
            finalizeOutputImage();
            mIsRecording = false;
            return;
        }
        Log.d(LOG_TAG, "Processing line " + mCurrentLine + " out of " + mMaxLine);

        // Convert byte array yuv to bitmap
        YuvImage yuv = new YuvImage(bytes, mCameraPreviewFormat, mCameraWidth, mCameraHeight, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, mCameraWidth, mCameraHeight), mJpegQuality, out);
        byte[] bitmapsBytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapsBytes, 0, bitmapsBytes.length);

        if(mCurrentLine == 0){
            mPixelLinePlace = (int) (float)bitmap.getWidth() / mPixelLinePlace;
        }

        // Iterate on the pixel line
        for(int y =0; y < bitmap.getHeight(); y ++){
            mOutputImage.setPixel(mCurrentLine, y, bitmap.getPixel(mPixelLinePlace, y));
        }
        mCurrentLine ++;
    }

    protected void finalizeOutputImage(){
        Log.d(LOG_TAG, "finalizeOutputImage");
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        VideoHelper.saveCurrentPicture(
                this,
                mOutputImage,
                sdf.format(new Date()) + ".jpg");
    }


    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                mIsRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                MainActivity.this.finish();
            }

        }
    }
}
