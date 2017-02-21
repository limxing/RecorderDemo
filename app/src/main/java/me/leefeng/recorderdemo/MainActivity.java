package me.leefeng.recorderdemo;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import me.leefeng.recorder.RecorderActivity;
import me.leefeng.recorderdemo.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//        setContentView(R.layout.activity_main);
        RecorderActivity.startActivityForResult(this, 0);
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                MY_PERMISSIONS_REQUEST_READ_CONTACTS);

        Kml kml = Kml.unmarshal(new File(""));
        Feature feature = kml.getFeature();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String path = data.getStringExtra("videoPath");
            String cameraPath = data.getStringExtra("cameraPath");
            String videoPicPath = data.getStringExtra("videoPicPath");


            mBinding.mainText.append("\n"+path + "\n" + cameraPath + "\n" + videoPicPath);

        }
    }
}
