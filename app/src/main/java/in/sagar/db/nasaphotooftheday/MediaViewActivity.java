package in.sagar.db.nasaphotooftheday;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MediaViewActivity extends AppCompatActivity {

    private String media_type, url;
    private ProgressDialog dialog;
    private ProgressBar progressBar;
    private VideoView mVideoView;
    private ImageView mImageView;
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView imageView;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_view);

        getSystemTheme();

        // Initializing View
        mImageView = findViewById(R.id.activity_media_view_image_view);
        mVideoView = findViewById(R.id.activity_media_view_video_view);
        progressBar = findViewById(R.id.activity_media_view_progressbar);


        // getting Intent Extras
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            media_type = bundle.getString("media_type");
            url = bundle.getString("url");
        } else {
            Toast.makeText(this, "Empty Bitmap", Toast.LENGTH_SHORT).show();
        }

        // if media type is video then play video
        if (media_type.equals("video")) {
            mVideoView.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            playVideo(url);

            // if media type is image view image
        } else if (media_type.equals("image")) {
            mVideoView.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);
            dialog = new ProgressDialog(MediaViewActivity.this);
            dialog.setMessage("Please wait...");
            dialog.setIcon(R.mipmap.ic_launcher);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            new ConvertURLImageToBitmap(mImageView).execute(url);
            scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    // Pinch to Zoom-in Zoom-out the image
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
            mImageView.setScaleX(mScaleFactor);
            mImageView.setScaleY(mScaleFactor);
            return true;
        }
    }

    // Play video from URL
    private void playVideo(String url) {
        Uri uri = Uri.parse(url);
        mVideoView.setVideoURI(uri);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mediaController);
        mVideoView.start();
        progressBar.setVisibility(View.VISIBLE);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int arg1, int arg2) {
                        progressBar.setVisibility(View.GONE);
                        mp.start();
                    }
                });
            }
        });
    }

    // Converting URL Image to Bitmap and view in ImageView
    @SuppressLint("StaticFieldLeak")
    public class ConvertURLImageToBitmap extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        Bitmap bitmap;

        public ConvertURLImageToBitmap(ImageView img) {
            this.imageView = img;
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            String stringUrl = url[0];
            bitmap = null;
            InputStream inputStream;
            try {
                inputStream = new java.net.URL(stringUrl).openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            imageView.setImageBitmap(bitmap);
            dialog.dismiss();
            dialog.hide();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getSystemTheme() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_YES:
                window.setStatusBarColor(getColor(R.color.black));
                break;
            case Configuration.UI_MODE_NIGHT_NO:
                window.setStatusBarColor(getColor(R.color.colorPrimary));
                break;
        }
    }
}