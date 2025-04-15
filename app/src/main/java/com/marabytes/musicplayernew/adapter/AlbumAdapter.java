package com.marabytes.musicplayernew.adapter;

import android.content.ContentUris;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marabytes.musicplayernew.R;
import com.marabytes.musicplayernew.model.Album;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<Album> albums;
    private OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public AlbumAdapter(List<Album> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.albumTitle.setText(album.getTitle());
        holder.albumArtist.setText(album.getArtist());
        
        // Load album art
        if (album.getAlbumArt() != null) {
            Uri albumArtUri = Uri.parse(album.getAlbumArt());
            holder.albumCover.setImageURI(albumArtUri);
            // Use a fallback if album art URI is null or not loading
            if (holder.albumCover.getDrawable() == null) {
                holder.albumCover.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.albumCover.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void updateAlbums(List<Album> newAlbums) {
        this.albums = newAlbums;
        notifyDataSetChanged();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView albumCover;
        TextView albumTitle;
        TextView albumArtist;

        AlbumViewHolder(View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.albumCover);
            albumTitle = itemView.findViewById(R.id.albumTitle);
            albumArtist = itemView.findViewById(R.id.albumArtist);
        }
    }
} 