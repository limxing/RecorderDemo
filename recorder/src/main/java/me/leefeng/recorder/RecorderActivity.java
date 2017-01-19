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
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;


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
                    startOrStop();
                    recorderCancle.setVisibility(View.VISIBLE);
                    recorderConfirm.setVisibility(View.VISIBLE);
                    recorderCap.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.recorder_cap).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                startOrStop();
                return true;
            }
        });
        findViewById(R.id.recorder_cap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myPictureCallback.isPreviewing) {
                    mCamera.startPreview();
                    myPictureCallback.setPreviewing(false);
                }
                mCamera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                }, null, myPictureCallback);
                recorderCancle.setVisibility(View.VISIBLE);
                recorderConfirm.setVisibility(View.VISIBLE);
                recorderCap.setVisibility(View.GONE);
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
            }
        });
        recorderConfirm = findViewById(R.id.recorder_confirm);
        recorderConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        recorderCap = findViewById(R.id.recorder_cap);


    }


    MyPictureCallback myPictureCallback = new MyPictureCallback();


    private final class MyPictureCallback implements Camera.PictureCallback {

        private byte[] picData;
        private boolean isPreviewing;

        public boolean isPreviewing() {
            return isPreviewing;
        }

        public void setPreviewing(boolean previewing) {
            isPreviewing = previewing;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            this.picData = data;
            camera.stopPreview(); // 拍完照后，重新开始预览
            try {
                String path = saveToSDCard(data); // 保存图片到sd卡中
                ExifInterface newExif = new ExifInterface(path);
                newExif.setAttribute("Orientation", "90");
                newExif.saveAttributes();

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

    private void startOrStop() {
        if (recording) {
            //如果正在录制点击这个按钮表示录制完成
            mediaRecorder.stop(); //停止
//                    stopChronometer();计时器
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            releaseMediaRecorder();
            recording = false;
            Bitmap bitmap = getVideoThumbnail(url_file, w, h, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
            try {
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
            runOnUiThread(new Runnable() {
                public void run() {
                    // If there are stories, add them to the table
                    try {
                        mediaRecorder.start();
//                                startChronometer();计时器
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
                }
            });
            recording = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        releaseMediaRecorder();
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
        if (mediaRecorder != null) {
            return true;
        }
        mediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (cameraFront) {
                mediaRecorder.setOrientationHint(270);
            } else {
                mediaRecorder.setOrientationHint(90);
            }
        }

        mediaRecorder.setProfile(CamcorderProfile.get(quality));
        File file = new File("/mnt/sdcard/videokit");
        if (!file.exists()) {
            file.mkdirs();
        }
        Date d = new Date();
        String timestamp = String.valueOf(d.getTime());
//        url_file = Environment.getExternalStorageDirectory().getPath() + "/videoKit" + timestamp + ".mp4";
        url_file = "/mnt/sdcard/videokit/in.mp4";
//        url_file = "/mnt/sdcard/videokit/" + timestamp + ".mp4";

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
//                mBinding.cameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                mBinding.buttonFlash.setImageResource(R.mipmap.ic_flash_on_white);
//            }
            } else if (!frontal) {
                cameraId = findBackFacingCamera();
//            if (flash) {
//                mBinding.cameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                mBinding.buttonFlash.setImageResource(R.mipmap.ic_flash_on_white);
//            }
            }

            mCamera = Camera.open(cameraId);
            cameraPreview.refreshCamera(mCamera);
            reloadQualities(cameraId);
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
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

}
