package me.leefeng.recorder;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by FengTing on 2017/1/18.
 */

public class RecorderActivity extends AppCompatActivity {
    private Compressor mCompressor;
    private Camera mCamera;
    private boolean cameraFront;
    private int quality;
    private boolean recording;
    private MediaRecorder mediaRecorder;
    private String url_file;
    private CameraPreview cameraPreview;

    private int h;
    private int w;
    private View recorderCancle;
    private View recorderConfirm;
    private View recorderCap;
    private String cameraPath;
    private Chronometer recorderTimer;
    private long countUp;
    private View recorderMovie;
    private String videoPicPath;

    public static void startActivityForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, RecorderActivity.class);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, null);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_recorder);
        w = getResources().getDisplayMetrics().widthPixels;
        h = getResources().getDisplayMetrics().heightPixels;
        Log.i("limxing", "onCreate: "+w+"=="+h);
        cameraPreview = (CameraPreview) findViewById(R.id.camera_preview);
        initView();

        cameraPreview.initCamera(mCamera);

        mCompressor = new Compressor(this);
        mCompressor.loadBinary(new InitListener() {
            @Override
            public void onLoadSuccess() {
//                Toast.makeText(RecorderActivity.this, "加载库成功", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onLoadFail(String reason) {
//                Toast.makeText(RecorderActivity.this, "加载库失败", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.recorder_cap).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && recording) {
                    cameraPath = null;
                    if (startOrStop()) {
                        recorderCancle.setVisibility(View.VISIBLE);
                        recorderConfirm.setVisibility(View.VISIBLE);
                        recorderCap.setVisibility(View.INVISIBLE);
                    }
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.recorder_cap).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                recorderMovie.setVisibility(View.VISIBLE);
                startOrStop();
                return true;
            }
        });
        findViewById(R.id.recorder_cap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorderCancle.getVisibility() == View.VISIBLE) {
                    return;
                }
                if (myPictureCallback.isPreviewing) {
                    myPictureCallback.setPreviewing(false);
                }
                mCamera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                }, null, myPictureCallback);
                recorderCancle.setVisibility(View.VISIBLE);
                recorderConfirm.setVisibility(View.VISIBLE);
                recorderCap.setVisibility(View.INVISIBLE);
            }
        });
        findViewById(R.id.recorder_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void initView() {
        recorderCancle = findViewById(R.id.recorder_cancle);
        recorderCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recorderCancle.setVisibility(View.GONE);
                recorderConfirm.setVisibility(View.GONE);
                recorderCap.setVisibility(View.VISIBLE);
                if (cameraPath != null)
                    new File(cameraPath).delete();
                mCamera.startPreview();
                if (url_file != null)
                    new File(url_file).delete();
                if (videoPicPath != null)
                    new File(videoPicPath).delete();
                cameraPath = null;
                url_file = null;
                videoPicPath = null;
            }
        });
        recorderConfirm = findViewById(R.id.recorder_confirm);
        recorderConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.putExtra("videoPath", url_file);
                intent.putExtra("cameraPath", cameraPath);
                intent.putExtra("videoPicPath", videoPicPath);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
        recorderCap = findViewById(R.id.recorder_cap);

        cameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setPressed(false);
                    try {
                        focusOnTouch(event);
                    } catch (Exception e) {
                    }
                }
                return true;
            }
        });
        recorderTimer = (Chronometer) findViewById(R.id.recorder_timer);
        recorderMovie = findViewById(R.id.recorder_movie);
    }

    //对焦
    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0) {
                Rect rect = calculateFocusArea(event.getX(), event.getY());
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(rect, 800));
                parameters.setFocusAreas(meteringAreas);


                mCamera.setParameters(parameters);
                mCamera.autoFocus(null);
            } else {
                mCamera.autoFocus(null);
            }
        }
    }

    private static final int FOCUS_AREA_SIZE = 500;

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / w) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / h) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + focusAreaSize / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - focusAreaSize / 2;
            } else {
                result = -1000 + focusAreaSize / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - focusAreaSize / 2;
        }
        return result;
    }

    MyPictureCallback myPictureCallback = new MyPictureCallback();


    private final class MyPictureCallback implements Camera.PictureCallback {

        private boolean isPreviewing;

        public boolean isPreviewing() {
            return isPreviewing;
        }

        public void setPreviewing(boolean previewing) {
            isPreviewing = previewing;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.stopPreview(); // 拍完照后，重新开始预览
            try {
                String path = saveToSDCard(data); // 保存图片到sd卡中
//                ExifInterface newExif = new ExifInterface(path);
//                newExif.setAttribute("Orientation", "90");
//                newExif.saveAttributes();
                url_file = null;
                Log.i("limxing", "onPictureTaken: "+w+"=="+h);
                cameraPath = BitmapHelper.compressBitmap(path, w, h, true, getPreviewDegree());
                isPreviewing = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将拍下来的照片存放在SD卡中
     *
     * @param data
     * @throws IOException
     */
    public String saveToSDCard(byte[] data) throws Exception {

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        File fileFolder = new File(Environment.getExternalStorageDirectory()
                + "/limxing/");
        if (!fileFolder.exists()) {
            fileFolder.mkdir();
        }
        File jpgFile = new File(fileFolder, filename);
        jpgFile.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        outputStream.write(data); // 写入sd卡中
        outputStream.close(); // 关闭输出流
        return jpgFile.getAbsolutePath();

    }

    //计时器
    private void startChronometer() {
        recorderTimer.setVisibility(View.VISIBLE);
        final long startTime = SystemClock.elapsedRealtime();
        recorderTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer arg0) {
                countUp = (SystemClock.elapsedRealtime() - startTime) / 1000;
//                if (countUp % 2 == 0) {
//                    recorderMovie.setVisibility(View.VISIBLE);
//                } else {
//                    recorderMovie.setVisibility(View.INVISIBLE);
//                }

                String asText = String.format("%02d", countUp / 60) + ":" + String.format("%02d", countUp % 60);
                recorderTimer.setText(asText);
            }
        });
        recorderTimer.start();
    }

    private void stopChronometer() {
        recorderTimer.stop();
        recorderMovie.setVisibility(View.INVISIBLE);
        recorderTimer.setVisibility(View.INVISIBLE);
    }

    private boolean startOrStop() {

        if (recording) {
            recording = false;
            stopChronometer();//计时器
            //如果正在录制点击这个按钮表示录制完成
//            mediaRecorder.setOnErrorListener(null);
//            mediaRecorder.setOnInfoListener(null);
//            mediaRecorder.setPreviewDisplay(null);
            try {
                mediaRecorder.stop(); //停止
            } catch (Exception e) {
                releaseMediaRecorder();
                Toast.makeText(this, "时间太短啦", Toast.LENGTH_LONG).show();
                if (url_file != null)
                    new File(url_file).delete();
                return false;
            }
//
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            releaseMediaRecorder();
            try {
                mCamera.stopPreview();
//                MediaPlayer mediaPlayer = new MediaPlayer();
//                mediaPlayer.setDataSource(url_file);
//                mediaPlayer.setDisplay(cameraPreview.getHolder());
//                mediaPlayer.prepare();
//                mediaPlayer.start();

                Bitmap bitmap = getVideoThumbnail(url_file, w, h, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);

                Date date = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
                String filename = format.format(date) + ".jpg";
                File fileFolder = new File(Environment.getExternalStorageDirectory()
                        + "/limxing/");
                if (!fileFolder.exists()) {
                    fileFolder.mkdir();
                }
                File jpgFile = new File(fileFolder, filename);
                FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                outputStream.close(); // 关闭输出流
                videoPicPath = jpgFile.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            Intent intent = new Intent();
//            intent.putExtra("path", url_file);
//            setResult(Activity.RESULT_OK, intent);
//
//            finish();
        } else {
            //准备开始录制视频
            if (!prepareMediaRecorder()) {
                setResult(Activity.RESULT_CANCELED);
                releaseCamera();
                releaseMediaRecorder();
                finish();
            }
            //开始录制视频
            try {
                mediaRecorder.start();
                startChronometer();//计时器
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            } catch (final Exception ex) {
                Log.i("---", "Exception in thread");
                setResult(Activity.RESULT_CANCELED);
                releaseCamera();
                releaseMediaRecorder();
                finish();
            }
            recording = true;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        releaseMediaRecorder();
        if (mediaRecorder != null)
            mediaRecorder.release();
        mediaRecorder = null;
    }

    /**
     * 获取视频截图
     *
     * @param videoPath
     * @param width
     * @param height
     * @param kind
     * @return
     */
    private Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                     int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        System.out.println("w" + bitmap.getWidth());
        System.out.println("h" + bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /**
     * 准备Media
     *
     * @return
     */
    private boolean prepareMediaRecorder() {
        mCamera.unlock();
        if (mediaRecorder == null)
            mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//            mediaRecorder.setPreviewDisplay(cameraPreview);
        mediaRecorder.setProfile(CamcorderProfile.get(quality));
        File file = new File("/mnt/sdcard/videokit");
        if (!file.exists()) {
            file.mkdirs();
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (cameraFront) {
                mediaRecorder.setOrientationHint(270);
            } else {
                mediaRecorder.setOrientationHint(90);
            }
        }
        url_file = "/mnt/sdcard/videokit/in.mp4";
        File file1 = new File(url_file);
        if (file1.exists()) {
            file1.delete();
        }
        mediaRecorder.setOutputFile(url_file);
//        https://developer.android.com/reference/android/media/MediaRecorder.html#setMaxDuration(int) 不设置则没有限制
//        mediaRecorder.setMaxDuration(CameraConfig.MAX_DURATION_RECORD); //设置视频文件最长时间 60s.
//        https://developer.android.com/reference/android/media/MediaRecorder.html#setMaxFileSize(int) 不设置则没有限制
//        mediaRecorder.setMaxFileSize(CameraConfig.MAX_FILE_SIZE_RECORD); //设置视频文件最大size 1G

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasCamera(getApplicationContext())) {
            //这台设备没有发现摄像头
            Toast.makeText(getApplicationContext(), "这台设备没有发现摄像头"
                    , Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_CANCELED);
            releaseCamera();
            releaseMediaRecorder();
            finish();
        }
        if (mCamera == null) {
            final boolean frontal = cameraFront;

            int cameraId = findFrontFacingCamera();
            if (cameraId < 0) {
                //前置摄像头不存在
//                switchCameraListener = new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Toast.makeText(CameraActivity.this, R.string.dont_have_front_camera, Toast.LENGTH_SHORT).show();
//                    }
//                };

                //尝试寻找后置摄像头
                cameraId = findBackFacingCamera();
//            if (flash) {
//                cameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                buttonFlash.setImageResource(R.mipmap.ic_flash_on_white);
//            }
            } else if (!frontal) {
                cameraId = findBackFacingCamera();
//            if (flash) {
//                cameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                buttonFlash.setImageResource(R.mipmap.ic_flash_on_white);
//            }
            }

            mCamera = Camera.open(cameraId);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> list=parameters.getSupportedPictureSizes();
//            for (Camera.Size size:list){
//                Log.i("limxing", "focusOnTouch: "+size.width+"=="+size.height);
//            }
            Camera.Size size=list.get(list.size()/2);
            parameters.setPictureSize(size.width, size.height); // 设置保存的图片尺寸
            parameters.setJpegQuality(100); // 设置照片质量
//
//            mCamera.getSupportedPictureSizes()得到当前所支持的照片大小，然后
//            parameters.setPictureSize(w, h);
            mCamera.setParameters(parameters);
            cameraPreview.refreshCamera(mCamera);
            reloadQualities(cameraId);
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            if (mCamera != null)
                mCamera.lock();
        }
    }

    //reload成像质量
    private void reloadQualities(int idCamera) {
        if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_720P)) {
            quality = CamcorderProfile.QUALITY_720P;
        } else if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_1080P)) {
            quality = CamcorderProfile.QUALITY_1080P;
        } else if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_480P)) {
            quality = CamcorderProfile.QUALITY_480P;
        } else if (CamcorderProfile.hasProfile(idCamera, CamcorderProfile.QUALITY_2160P)) {
            quality = CamcorderProfile.QUALITY_2160P;
        }
        Toast.makeText(this,quality+"",Toast.LENGTH_LONG).show();
        Camera.Parameters parameters = mCamera.getParameters();
//获取摄像头支持的各种分辨率
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        for (Camera.Size size:supportedPictureSizes){
            Log.i("limxing", "reloadQualities: "+size.toString());
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    //检查设备是否有摄像头
    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 找前置摄像头,没有则返回-1
     *
     * @return cameraId
     */
    private int findFrontFacingCamera() {
        int cameraId = -1;
        //获取摄像头个数
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    /**
     * 找后置摄像头,没有则返回-1
     *
     * @return cameraId
     */
    private int findBackFacingCamera() {
        int cameraId = -1;
        //获取摄像头个数
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    public int getPreviewDegree() {
        // 获得手机的方向
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        return degree;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (cameraPath != null)
            new File(cameraPath).delete();
        mCamera.startPreview();
        if (url_file != null)
            new File(url_file).delete();
        if (videoPicPath != null)
            new File(videoPicPath).delete();
    }
}
