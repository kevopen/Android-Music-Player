package com.marabytes.musicplayernew;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.marabytes.musicplayernew.model.Song;

public class PlayerActivity extends AppCompatActivity implements MediaPlayerManager.OnPlaybackChangeListener {
    private static final String EXTRA_SONG_PATH = "songPath";
    private static final String EXTRA_ALBUM_ID = "albumId";

    private ImageView albumArtImageView;
    private TextView songTitleTextView;
    private TextView artistTextView;
    private SeekBar seekBar;
    private ImageButton prevButton;
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private String currentSongPath;
    private String albumId;
    
    private MediaPlayerManager mediaPlayerManager;

    public static Intent newIntent(Context context, Song song, long albumId) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(EXTRA_SONG_PATH, song.getPath());
        intent.putExtra(EXTRA_ALBUM_ID, String.valueOf(albumId));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        currentSongPath = getIntent().getStringExtra(EXTRA_SONG_PATH);
        albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);

        mediaPlayerManager = MediaPlayerManager.getInstance();

        initializeViews();
        loadSongDetails();
        setupClickListeners();
        
        // Register as listener
        mediaPlayerManager.addListener(this);
    }

    private void initializeViews() {
        albumArtImageView = findViewById(R.id.albumArtImageView);
        songTitleTextView = findViewById(R.id.songTitleTextView);
        artistTextView = findViewById(R.id.artistTextView);
        seekBar = findViewById(R.id.seekBar);
        prevButton = findViewById(R.id.prevButton);
        playPauseButton = findViewById(R.id.playPauseButton);
        nextButton = findViewById(R.id.nextButton);

        // Set initial play/pause button state
        updatePlayPauseButton(mediaPlayerManager.isPlaying());
    }

    private void loadSongDetails() {
        String[] projection = {
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        };
        
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {currentSongPath};
        
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

                songTitleTextView.setText(title);
                artistTextView.setText(artist);

                if (albumId != null) {
                    Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), Long.parseLong(albumId));
                    Glide.with(this)
                            .load(albumArtUri)
                            .error(R.drawable.noart)
                            .into(albumArtImageView);
                }
            }
        }
    }

    private void setupClickListeners() {
        playPauseButton.setOnClickListener(v -> mediaPlayerManager.togglePlayPause());
        prevButton.setOnClickListener(v -> restartSong());
        nextButton.setOnClickListener(v -> finish());
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayerManager.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void restartSong() {
        mediaPlayerManager.seekTo(0);
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        playPauseButton.setImageResource(isPlaying ? 
            android.R.drawable.ic_media_pause : 
            android.R.drawable.ic_media_play);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updatePlayPauseButton(isPlaying);
    }

    @Override
    public void onProgressChanged(int progress, int duration) {
        seekBar.setMax(duration);
        seekBar.setProgress(progress);
    }

    @Override
    public void onSongChanged(String songPath) {
        if (!songPath.equals(currentSongPath)) {
            finish(); // Close activity if a different song is played
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayerManager.removeListener(this);
    }
} 