package com.marabytes.musicplayernew;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.marabytes.musicplayernew.adapter.SongAdapter;
import com.marabytes.musicplayernew.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ALBUM_ID = "album_id";
    private static final String EXTRA_ALBUM_TITLE = "album_title";
    private static final String EXTRA_ALBUM_ARTIST = "album_artist";
    private static final String EXTRA_ALBUM_ART_PATH = "album_art_path";
    private static final int DELETE_PERMISSION_REQUEST_CODE = 101;

    private ImageView albumArtImageView;
    private TextView albumTitleTextView;
    private TextView albumArtistTextView;
    private RecyclerView songsRecyclerView;
    private SongAdapter songAdapter;
    private List<Song> songs;
    private MediaPlayerManager mediaPlayerManager;
    private Song songPendingDeletion;
    private MediaPlayerManager.OnPlaybackChangeListener playbackChangeListener;

    // Player controls
    private ImageButton btnPlayPause, btnPrevious, btnNext;
    private TextView currentSongTitle, currentSongArtist;
    private ImageView bannerImage;

    public static Intent newIntent(Context context, String albumId, String title, String artist, String albumArtPath) {
        Intent intent = new Intent(context, AlbumDetailActivity.class);
        intent.putExtra(EXTRA_ALBUM_ID, albumId);
        intent.putExtra(EXTRA_ALBUM_TITLE, title);
        intent.putExtra(EXTRA_ALBUM_ARTIST, artist);
        intent.putExtra(EXTRA_ALBUM_ART_PATH, albumArtPath);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        mediaPlayerManager = MediaPlayerManager.getInstance();

        String albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        String title = getIntent().getStringExtra(EXTRA_ALBUM_TITLE);
        String artist = getIntent().getStringExtra(EXTRA_ALBUM_ARTIST);
        String albumArtPath = getIntent().getStringExtra(EXTRA_ALBUM_ART_PATH);

        initializeViews();
        setupPlayerControls();
        setupAlbumInfo(title, artist, albumArtPath);
        loadAlbumSongs(albumId);
    }

    private void initializeViews() {
        albumArtImageView = findViewById(R.id.albumArt);
        albumTitleTextView = findViewById(R.id.albumTitle);
        albumArtistTextView = findViewById(R.id.albumArtist);
        songsRecyclerView = findViewById(R.id.albumSongsRecyclerView);

        // Initialize player controls
        View playerControls = findViewById(R.id.playerControls);
        btnPlayPause = playerControls.findViewById(R.id.btnPlayPause);
        btnPrevious = playerControls.findViewById(R.id.btnPrevious);
        btnNext = playerControls.findViewById(R.id.btnNext);
        currentSongTitle = playerControls.findViewById(R.id.currentSongTitle);
        currentSongArtist = playerControls.findViewById(R.id.currentSongArtist);
        bannerImage = playerControls.findViewById(R.id.bannerImage);

        songs = new ArrayList<>();
        songAdapter = new SongAdapter(songs, this::playSong, this::showSongMenu);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songsRecyclerView.setAdapter(songAdapter);
    }

    private void setupPlayerControls() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());

        // Create and store the listener
        playbackChangeListener = new MediaPlayerManager.OnPlaybackChangeListener() {
            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(isPlaying ? 
                    android.R.drawable.ic_media_pause : 
                    android.R.drawable.ic_media_play);
            }

            @Override
            public void onProgressChanged(int progress, int duration) {
                // Not needed for basic controls
            }

            @Override
            public void onSongChanged(String songPath) {
                // Update current song info
                for (Song song : songs) {
                    if (song.getPath().equals(songPath)) {
                        currentSongTitle.setText(song.getTitle());
                        currentSongArtist.setText(song.getArtist());
                        break;
                    }
                }
            }
        };

        // Register the stored listener
        mediaPlayerManager.addListener(playbackChangeListener);

        // Set initial state
        if (mediaPlayerManager.getCurrentSongPath() != null) {
            for (Song song : songs) {
                if (song.getPath().equals(mediaPlayerManager.getCurrentSongPath())) {
                    currentSongTitle.setText(song.getTitle());
                    currentSongArtist.setText(song.getArtist());
                    btnPlayPause.setImageResource(mediaPlayerManager.isPlaying() ? 
                        android.R.drawable.ic_media_pause : 
                        android.R.drawable.ic_media_play);
                    break;
                }
            }
        }
    }

    private void togglePlayPause() {
        mediaPlayerManager.togglePlayPause();
    }

    private void playPrevious() {
        int currentIndex = -1;
        String currentPath = mediaPlayerManager.getCurrentSongPath();
        
        if (currentPath != null) {
            for (int i = 0; i < songs.size(); i++) {
                if (songs.get(i).getPath().equals(currentPath)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        if (currentIndex > 0) {
            playSong(songs.get(currentIndex - 1));
        }
    }

    private void playNext() {
        int currentIndex = -1;
        String currentPath = mediaPlayerManager.getCurrentSongPath();
        
        if (currentPath != null) {
            for (int i = 0; i < songs.size(); i++) {
                if (songs.get(i).getPath().equals(currentPath)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        if (currentIndex < songs.size() - 1) {
            playSong(songs.get(currentIndex + 1));
        }
    }

    private void setupAlbumInfo(String title, String artist, String albumArtPath) {
        albumTitleTextView.setText(title);
        albumArtistTextView.setText(artist);

        if (albumArtPath != null) {
            Uri albumArtUri = Uri.parse(albumArtPath);
            Glide.with(this)
                    .load(albumArtUri)
                    .error(R.drawable.noart)
                    .into(albumArtImageView);
        } else {
            albumArtImageView.setImageResource(R.drawable.noart);
        }
    }

    private void loadAlbumSongs(String albumId) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.ALBUM_ID + "=?";
        String[] selectionArgs = {albumId};
        String sortOrder = MediaStore.Audio.Media.TRACK;

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                    songs.add(new Song(title, artist, path, duration));
                }
                songAdapter.updateSongs(songs);
            }
        }
    }

    private void playSong(Song song) {
        try {
            // Get album art URI
            Uri albumArtUri = null;
            String[] projection = {MediaStore.Audio.Media.ALBUM_ID};
            String selection = MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = new String[]{song.getPath()};
            
            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                    albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId);
                }
            }

            // Start playback
            mediaPlayerManager.playSong(song.getPath(), song.getTitle(), song.getArtist(), albumArtUri);

            // Start the service
            Intent serviceIntent = new Intent(this, MusicService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error playing song", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSongMenu(Song song, android.view.View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.song_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_share) {
                shareSong(song);
                return true;
            } else if (itemId == R.id.action_delete) {
                deleteSong(song);
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void shareSong(Song song) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        Uri songUri = Uri.parse(song.getPath());
        shareIntent.putExtra(Intent.EXTRA_STREAM, songUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(Intent.createChooser(shareIntent, "Share " + song.getTitle()));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share song", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSong(Song song) {
        // Reuse the delete functionality from MainActivity
        // You should consider moving this to a utility class
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Song")
            .setMessage("Are you sure you want to delete '" + song.getTitle() + "'?")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    deleteWithMediaStore(song);
                } else {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        songPendingDeletion = song;
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                DELETE_PERMISSION_REQUEST_CODE);
                    } else {
                        performDeleteSong(song);
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteWithMediaStore(Song song) {
        try {
            ContentResolver contentResolver = getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = new String[]{song.getPath()};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Query for the media ID first
                String[] projection = {MediaStore.Audio.Media._ID};
                try (Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                        Uri deleteUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                        PendingIntent pi = MediaStore.createDeleteRequest(contentResolver, 
                            Collections.singletonList(deleteUri));
                        startIntentSenderForResult(pi.getIntentSender(), DELETE_PERMISSION_REQUEST_CODE,
                                null, 0, 0, 0);
                        songPendingDeletion = song;
                    }
                }
            } else {
                int rowsDeleted = contentResolver.delete(uri, selection, selectionArgs);
                if (rowsDeleted > 0) {
                    handleSuccessfulDeletion(song);
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error deleting song", Toast.LENGTH_SHORT).show();
        }
    }

    private void performDeleteSong(Song song) {
        try {
            String filePath = song.getPath();
            java.io.File file = new java.io.File(filePath);

            // Delete from MediaStore first
            String where = MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = { filePath };
            getContentResolver().delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                where,
                selectionArgs);

            // Then delete the actual file
            if (file.exists() && file.delete()) {
                handleSuccessfulDeletion(song);
            } else {
                Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error deleting song", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSuccessfulDeletion(Song song) {
        int position = songs.indexOf(song);
        if (position != -1) {
            songs.remove(position);
            songAdapter.notifyItemRemoved(position);
            
            // If this was the current song playing, stop it
            if (mediaPlayerManager != null && 
                song.getPath().equals(mediaPlayerManager.getCurrentSongPath())) {
                mediaPlayerManager.release();
                mediaPlayerManager = MediaPlayerManager.getInstance();
            }
        }
        Toast.makeText(this, "Song deleted successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
                                         @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == DELETE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (songPendingDeletion != null) {
                    performDeleteSong(songPendingDeletion);
                    songPendingDeletion = null;
                }
            } else {
                Toast.makeText(this, "Permission denied. Cannot delete files.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DELETE_PERMISSION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (songPendingDeletion != null) {
                    handleSuccessfulDeletion(songPendingDeletion);
                    songPendingDeletion = null;
                }
            } else {
                Toast.makeText(this, "Delete permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayerManager != null && playbackChangeListener != null) {
            mediaPlayerManager.removeListener(playbackChangeListener);
        }
    }
} 