package com.sum.ladybuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private Context context;
    private List<VideoModel> videoList;

    public VideoAdapter(Context context, List<VideoModel> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoModel video = videoList.get(position);
        holder.title.setText(video.getVideoTitle());

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof SelfDef) {
                ((SelfDef) context).playSelectedVideo(video.getVideoUri());
            }
        });
    }

    @Override
    public int getItemCount() { 
        return videoList != null ? videoList.size() : 0; 
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView thumbnail;
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.videoTitleText);
            thumbnail = itemView.findViewById(R.id.videoThumbnail);
        }
    }
}