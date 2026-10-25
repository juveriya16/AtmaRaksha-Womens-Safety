package com.sum.ladybuddy;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SelfDef extends AppCompatActivity {
    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private VideoView videoViewPlayer;
    private View llVideoHint;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_def);

        btnBack = findViewById(R.id.btn_back_selfdef);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        videoViewPlayer = findViewById(R.id.videoViewPlayer);
        llVideoHint = findViewById(R.id.ll_video_hint);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        videoList = new ArrayList<>();
        videoList.add(new VideoModel("Best Self Defence Move", getVideoUri(R.raw.video1)));
        videoList.add(new VideoModel("Veerangana | Self-Defence", getVideoUri(R.raw.video2)));
        videoList.add(new VideoModel("Self Defence Tips", getVideoUri(R.raw.video4)));
        videoList.add(new VideoModel("Hair Grab Defences", getVideoUri(R.raw.video5)));

        videoAdapter = new VideoAdapter(this, videoList);
        recyclerView.setAdapter(videoAdapter);
    }

    public void playSelectedVideo(String uriString) {
        if (videoViewPlayer != null) {
            if (llVideoHint != null) {
                llVideoHint.setVisibility(View.GONE);
            }
            videoViewPlayer.setVideoURI(Uri.parse(uriString));
            MediaController mc = new MediaController(this);
            videoViewPlayer.setMediaController(mc);
            mc.setAnchorView(videoViewPlayer);
            videoViewPlayer.start();
        }
    }

    private String getVideoUri(int resId) {
        return "android.resource://" + getPackageName() + "/" + resId;
    }
}