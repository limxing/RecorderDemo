package me.leefeng.recorderdemo;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import me.leefeng.recorder.RecorderActivity;
import me.leefeng.recorderdemo.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//        setContentView(R.layout.activity_main);
        RecorderActivity.startActivityForResult(this, 0);
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
