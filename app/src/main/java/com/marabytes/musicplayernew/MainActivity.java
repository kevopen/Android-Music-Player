package com.marabytes.musicplayernew;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.Intent;

import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.marabytes.musicplayernew.adapter.AlbumAdapter;
import com.marabytes.musicplayernew.adapter.SongAdapter;
import com.marabytes.musicplayernew.model.Album;
import com.marabytes.musicplayernew.model.Song;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int DELETE_PERMISSION_REQUEST_CODE = 101;
    private RecyclerView songsRecyclerView, albumsRecyclerView;
    private SongAdapter songAdapter;
    private AlbumAdapter albumAdapter;
    private List<Song> songs;
    private List<Album> albums;
    private ImageButton btnPlayPause, btnPrevious, btnNext;
    private TextView currentSongTitle, currentSongArtist;
    private ImageView bannerImage;
    private TextView seeAllSongs, seeAllAlbums;
    private MediaPlayerManager mediaPlayerManager;
    private int currentSongIndex = -1;
    private Song songPendingDeletion = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mediaPlayerManager = MediaPlayerManager.getInstance();
        initializeViews();
        checkPermissions();
    }

    private void initializeViews() {
        songsRecyclerView = findViewById(R.id.songsRecyclerView);
        albumsRecyclerView = findViewById(R.id.albumsRecyclerView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        currentSongTitle = findViewById(R.id.currentSongTitle);
        currentSongArtist = findViewById(R.id.currentSongArtist);
        bannerImage = findViewById(R.id.bannerImage);
        seeAllSongs = findViewById(R.id.seeAllSongs);
        seeAllAlbums = findViewById(R.id.seeAllAlbums);

        songs = new ArrayList<>();
        albums = new ArrayList<>();
        
        songAdapter = new SongAdapter(songs, this::playSong, this::showSongMenu);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songsRecyclerView.setAdapter(songAdapter);
        
        albumAdapter = new AlbumAdapter(albums, this::onAlbumClick);
        albumsRecyclerView.setAdapter(albumAdapter);

        setupPlayerControls();
        setupClickListeners();

        // Make the player controls clickable to open full player
        findViewById(R.id.playerContainer).setOnClickListener(v -> {
            if (currentSongIndex >= 0 && currentSongIndex < songs.size()) {
                Song currentSong = songs.get(currentSongIndex);
                // Get the album ID for the current song
                long albumId = getAlbumIdForSong(currentSong.getPath());
                Intent intent = PlayerActivity.newIntent(this, currentSong, albumId);
                startActivity(intent);
            }
        });
    }

    private void setupClickListeners() {
        seeAllSongs.setOnClickListener(v -> Toast.makeText(this, "Show all songs", Toast.LENGTH_SHORT).show());
        seeAllAlbums.setOnClickListener(v -> Toast.makeText(this, "Show all albums", Toast.LENGTH_SHORT).show());
    }

    private void onAlbumClick(Album album) {
        Intent intent = AlbumDetailActivity.newIntent(
            this,
            album.getId(),
            album.getTitle(),
            album.getArtist(),
            album.getAlbumArt()
        );
        startActivity(intent);
    }

    private void setupPlayerControls() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());

        // Register for playback updates
        mediaPlayerManager.addListener(new MediaPlayerManager.OnPlaybackChangeListener() {
            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(isPlaying ? 
                    android.R.drawable.ic_media_pause : 
                    android.R.drawable.ic_media_play);
            }

            @Override
            public void onProgressChanged(int progress, int duration) {
                // You could add a seekbar to the main activity if desired
            }

            @Override
            public void onSongChanged(String songPath) {
                // Update current song index if needed
                for (int i = 0; i < songs.size(); i++) {
                    if (songs.get(i).getPath().equals(songPath)) {
                        currentSongIndex = i;
                        Song song = songs.get(i);
                        currentSongTitle.setText(song.getTitle());
                        currentSongArtist.setText(song.getArtist());
                        break;
                    }
                }
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMediaContent();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ needs to use MediaStore API
            loadMediaContent();
        } else {
            // For Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMediaContent();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMediaContent();
            } else {
                Toast.makeText(this, "Permission denied. App cannot access music files.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == DELETE_PERMISSION_REQUEST_CODE) {
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

    private void loadMediaContent() {
        loadAlbums();
        loadSongs();
    }

    private void loadAlbums() {
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ART
        };

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String title = cursor.getString(1);
                    String artist = cursor.getString(2);
                    String albumArt = cursor.getString(3);

                    // If album art is null, try to find it from any song in the album
                    if (albumArt == null) {
                        albumArt = findAlbumArtFromSongs(id);
                    }

                    albums.add(new Album(title, artist, albumArt, id));
                }
                albumAdapter.updateAlbums(albums);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String findAlbumArtFromSongs(String albumId) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.ALBUM_ID
        };
        String selection = MediaStore.Audio.Media.ALBUM_ID + "=?";
        String[] selectionArgs = new String[]{albumId};

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadSongs() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String title = cursor.getString(1);
                    String artist = cursor.getString(2);
                    String path = cursor.getString(3);
                    long duration = cursor.getLong(4);
                    long albumId = cursor.getLong(5);

                    songs.add(new Song(title, artist, path, duration));
                    
                    // Use the first song's album art for the banner if available
                    if (songs.size() == 1) {
                        Uri albumArtUri = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId);
                        bannerImage.setImageURI(albumArtUri);
                        if (bannerImage.getDrawable() == null) {
                            bannerImage.setImageResource(android.R.drawable.ic_menu_gallery);
                        }
                    }
                }
                songAdapter.updateSongs(songs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSong(Song song) {
        try {
            // Get album art URI
            Uri albumArtUri = null;
            long albumId = getAlbumIdForSong(song.getPath());
            if (albumId != -1) {
                albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId);
            }

            // Start playback
            mediaPlayerManager.playSong(song.getPath(), song.getTitle(), song.getArtist(), albumArtUri);
            currentSongTitle.setText(song.getTitle());
            currentSongArtist.setText(song.getArtist());
            currentSongIndex = songs.indexOf(song);

            // Update play/pause button
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);

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

    private void showSongMenu(Song song, View view) {
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
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Song")
            .setMessage("Are you sure you want to delete '" + song.getTitle() + "'?")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // For Android 11 and above, use MediaStore API
                    deleteWithMediaStore(song);
                } else {
                    // For Android 10 and below, check for permission
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
            // Create a delete request for MediaStore
            ContentResolver contentResolver = getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = new String[]{song.getPath()};

            // Request user's permission for deletion through system UI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PendingIntent pi = MediaStore.createDeleteRequest(contentResolver,
                    Collections.singletonList(Uri.parse(song.getPath())));
                try {
                    startIntentSenderForResult(pi.getIntentSender(), DELETE_PERMISSION_REQUEST_CODE,
                            null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            } else {
                // For Android 10, we can delete directly through MediaStore
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
        // Remove from our list and notify adapter
        int position = songs.indexOf(song);
        if (position != -1) {
            songs.remove(position);
            songAdapter.notifyItemRemoved(position);
            
            // If this was the current song playing, stop it
            if (mediaPlayerManager != null && 
                song.getPath().equals(mediaPlayerManager.getCurrentSongPath())) {
                mediaPlayerManager.release();
                mediaPlayerManager = MediaPlayerManager.getInstance();
                setupPlayerControls();
            }
        }
        Toast.makeText(this, "Song deleted successfully", Toast.LENGTH_SHORT).show();
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

    private void togglePlayPause() {
        mediaPlayerManager.togglePlayPause();
    }

    private void playPrevious() {
        if (currentSongIndex > 0) {
            playSong(songs.get(--currentSongIndex));
        }
    }

    private void playNext() {
        if (currentSongIndex < songs.size() - 1) {
            playSong(songs.get(++currentSongIndex));
        }
    }

    private long getAlbumIdForSong(String songPath) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media.ALBUM_ID};
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = new String[]{songPath};

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayerManager.release();
    }
}