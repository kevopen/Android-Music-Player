package com.marabytes.musicplayernew.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marabytes.musicplayernew.R;
import com.marabytes.musicplayernew.model.Song;

import java.io.IOException;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<Song> songs;
    private OnSongClickListener listener;
    private OnMenuClickListener menuListener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public interface OnMenuClickListener {
        void onMenuClick(Song song, View view);
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener, OnMenuClickListener menuListener) {
        this.songs = songs;
        this.listener = listener;
        this.menuListener = menuListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.songTitle.setText(song.getTitle());
        holder.songArtist.setText(song.getArtist());
        
        // Load album art
        loadAlbumArt(holder.songAlbumArt, song.getPath());
        
        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
        holder.songMenu.setOnClickListener(v -> menuListener.onMenuClick(song, v));
    }

    private void loadAlbumArt(ImageView imageView, String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Exception e) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                // Ignore release exception
            }
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView songAlbumArt;
        TextView songTitle;
        TextView songArtist;
        ImageButton songMenu;

        SongViewHolder(View itemView) {
            super(itemView);
            songAlbumArt = itemView.findViewById(R.id.songAlbumArt);
            songTitle = itemView.findViewById(R.id.songTitle);
            songArtist = itemView.findViewById(R.id.songArtist);
            songMenu = itemView.findViewById(R.id.songMenu);
        }
    }
} 