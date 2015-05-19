package xjet.fr.pixaline;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// TODO : http://stackoverflow.com/questions/1817742/how-can-i-capture-a-video-recording-on-android
@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    public static final String LOG_TAG = "MainActivity";

    protected Camera mCamera;

    protected SurfaceView mSurfaceView;
    protected SurfaceHolder mSurfaceHolder;

    protected Camera.PictureCallback mPictureCallback;

    protected String        mCurrentImageName;
    protected int           mCapturedFrame = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        mCurrentImageName = sdf.format(new Date());

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mSurfaceHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mPictureCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                MainActivity.this.saveCurrentPicture(data);
                Toast.makeText(getApplicationContext(), "Picture Saved", Toast.LENGTH_SHORT).show();
                refreshCamera();
            }
        };

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

    public void captureImage(View v) throws IOException {
        //take the picture
        mCamera.takePicture(null, null, mPictureCallback);
    }

    public void refreshCamera() {
        if (mSurfaceHolder.getSurface() == null) {
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception ignored) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        setupCamera();
    }

    private void setupCamera(){
        // modify parameter
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size cs = sizes.get(0);
        parameters.setPreviewSize(cs.width, cs.height);
        mCamera.setParameters(parameters);

        setCameraDisplayOrientation();

        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCameraDisplayOrientation(){
        if (mCamera == null) {
            Log.d(LOG_TAG, "setCameraDisplayOrientation - camera null");
            return;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(getCameraInformationId(), info);

        WindowManager winManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        int rotation = winManager.getDefaultDisplay().getRotation();

        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private int getCameraInformationId(){
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Log.d(LOG_TAG, "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    // Save given byte to tempt external dir
    private void saveCurrentPicture(byte[] data){

        mCapturedFrame ++;

        try {

            File imageFile = new File(this.getExternalCacheDir(), mCurrentImageName + mCapturedFrame + ".jpg");

            if(!imageFile.exists()){
                FileOutputStream output = new FileOutputStream(imageFile);
                InputStream inputStream = new ByteArrayInputStream(data); ;
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
                inputStream.close();
                output.close();
            }

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    /*********************************************
     * Implement SurfaceHolder.Callback
     *********************************************/

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            mCamera = Camera.open();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        setupCamera();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
}
