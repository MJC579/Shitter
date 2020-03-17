package org.nuclearfog.twidda.activity;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.ImageAdapter;
import org.nuclearfog.twidda.adapter.ImageAdapter.OnImageClickListener;
import org.nuclearfog.twidda.backend.ImageLoader;
import org.nuclearfog.zoomview.ZoomView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static org.nuclearfog.twidda.backend.ImageLoader.Action.ONLINE;
import static org.nuclearfog.twidda.backend.ImageLoader.Action.STORAGE;


public class MediaViewer extends AppCompatActivity implements OnImageClickListener, OnPreparedListener {

    public static final String KEY_MEDIA_LINK = "media_link";
    public static final String KEY_MEDIA_TYPE = "media_type";

    public static final int MEDIAVIEWER_IMAGE = 0;
    public static final int MEDIAVIEWER_VIDEO = 1;
    public static final int MEDIAVIEWER_ANGIF = 2;
    public static final int MEDIAVIEWER_IMG_STORAGE = 3;
    public static final int MEDIAVIEWER_VIDEO_STORAGE = 4;
    public static final int MEDIAVIEWER_ANGIF_STORAGE = 5;

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.GERMANY);
    private static final String[] REQ_WRITE_SD = {WRITE_EXTERNAL_STORAGE};
    private static final int REQCODE_SD = 6;

    private ImageLoader imageAsync;
    private ProgressBar video_progress;
    private ProgressBar image_progress;
    private MediaController videoController;
    private ImageAdapter adapter;
    private VideoView videoView;
    private ZoomView zoomImage;
    private String[] link;
    private int type;
    private int width;
    private int lastPos = 0;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.page_media);
        RecyclerView imageList = findViewById(R.id.image_list);
        View imageWindow = findViewById(R.id.image_window);
        View videoWindow = findViewById(R.id.video_window);
        image_progress = findViewById(R.id.image_load);
        video_progress = findViewById(R.id.video_load);
        zoomImage = findViewById(R.id.image_full);
        videoView = findViewById(R.id.video_view);
        videoController = new MediaController(this);
        adapter = new ImageAdapter(this);

        Bundle param = getIntent().getExtras();
        if (param != null && param.containsKey(KEY_MEDIA_LINK) && param.containsKey(KEY_MEDIA_TYPE)) {
            link = param.getStringArray(KEY_MEDIA_LINK);
            type = param.getInt(KEY_MEDIA_TYPE);
        }

        switch (type) {
            case MEDIAVIEWER_IMAGE:
            case MEDIAVIEWER_IMG_STORAGE:
            case MEDIAVIEWER_ANGIF_STORAGE:
                imageWindow.setVisibility(VISIBLE);
                imageList.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
                imageList.setAdapter(adapter);
                Display d = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                d.getSize(size);
                width = size.x;
                break;

            case MEDIAVIEWER_ANGIF:
                videoWindow.setVisibility(VISIBLE);
                Uri video = Uri.parse(link[0]);
                videoView.setOnPreparedListener(this);
                videoView.setVideoURI(video);
                break;

            case MEDIAVIEWER_VIDEO:
            case MEDIAVIEWER_VIDEO_STORAGE:
                videoWindow.setVisibility(VISIBLE);
                video = Uri.parse(link[0]);
                videoView.setMediaController(videoController);
                videoView.setOnPreparedListener(this);
                videoView.setVideoURI(video);
                break;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        switch (type) {
            case MEDIAVIEWER_IMAGE:
                if (imageAsync == null) {
                    imageAsync = new ImageLoader(this, ONLINE);
                    imageAsync.execute(link);
                }

                break;

            case MEDIAVIEWER_ANGIF_STORAGE:
            case MEDIAVIEWER_IMG_STORAGE:
                if (imageAsync == null) {
                    imageAsync = new ImageLoader(this, STORAGE);
                    imageAsync.execute(link);
                }
                break;

            case MEDIAVIEWER_VIDEO:
            case MEDIAVIEWER_ANGIF:
            case MEDIAVIEWER_VIDEO_STORAGE:
                videoView.start();
                break;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (type == MEDIAVIEWER_VIDEO || type == MEDIAVIEWER_VIDEO_STORAGE) {
            lastPos = videoView.getCurrentPosition();
            videoView.pause();
        }
    }


    @Override
    protected void onDestroy() {
        if (imageAsync != null && imageAsync.getStatus() == Status.RUNNING)
            imageAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onImageClick(Bitmap image) {
        setImage(image);
    }


    @Override
    public void onImageTouch(Bitmap image) {
        if (type == MEDIAVIEWER_IMAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int check = checkSelfPermission(WRITE_EXTERNAL_STORAGE);
                if (check == PERMISSION_GRANTED) {
                    storeImage(image);
                } else {
                    requestPermissions(REQ_WRITE_SD, REQCODE_SD);
                }
            } else {
                storeImage(image);
            }
        }
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        switch (type) {
            case MEDIAVIEWER_ANGIF:
                mp.setLooping(true);
                mp.start();
                break;

            case MEDIAVIEWER_VIDEO:
            case MEDIAVIEWER_VIDEO_STORAGE:
                videoController.show(0);
                mp.seekTo(lastPos);
                mp.start();
                break;
        }
        mp.setOnInfoListener(new OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MEDIA_INFO_VIDEO_RENDERING_START) {
                    video_progress.setVisibility(INVISIBLE);
                    return true;
                }
                return false;
            }
        });
    }


    public ImageAdapter getAdapter() {
        return adapter;
    }


    public void disableProgressbar() {
        image_progress.setVisibility(View.INVISIBLE);
    }


    public void setImage(@NonNull Bitmap image) {
        float ratio = image.getWidth() / (float) width;
        int destHeight = (int) (image.getHeight() / ratio);
        image = Bitmap.createScaledBitmap(image, width, destHeight, false);
        zoomImage.reset();
        zoomImage.setImageBitmap(image);
    }


    private void storeImage(Bitmap image) {
        String name = "shitter_" + formatter.format(new Date());
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(), image, name, "");
            Toast.makeText(this, R.string.image_saved, Toast.LENGTH_LONG).show();
        } catch (Exception err) {
            Toast.makeText(this, R.string.image_store_failure, Toast.LENGTH_SHORT).show();
        }
    }
}