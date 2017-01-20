package me.leefeng.recorder;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.SoftReference;

/**
 * Created by FengTing on 2017/1/20.
 */

public class VideoImage extends ImageView {
    private MyRunable runnable;

    public VideoImage(Context context) {
        super(context);
    }

    public VideoImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        runnable = new MyRunable(this);
        runnable.run();
    }

    static class MyRunable implements Runnable {
        private SoftReference<VideoImage> loadingViewSoftReference;
        private boolean visiable;

        public MyRunable(VideoImage loadingView) {
            loadingViewSoftReference = new SoftReference<VideoImage>(loadingView);
        }

        @Override
        public void run() {
            if (loadingViewSoftReference.get() != null && loadingViewSoftReference.get().runnable != null) {
                if (visiable) {
                    visiable = false;
                    loadingViewSoftReference.get().setVisibility(View.VISIBLE);
                } else {
                    visiable = true;
                    loadingViewSoftReference.get().setVisibility(View.INVISIBLE);
                }
                loadingViewSoftReference.get().postDelayed(loadingViewSoftReference.get().runnable, 500);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        runnable = null;
        super.onDetachedFromWindow();
    }
}
