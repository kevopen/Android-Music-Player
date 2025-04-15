package com.marabytes.musicplayernew;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerManager {
    private static MediaPlayerManager instance;
    private MediaPlayer mediaPlayer;
    private String currentSongPath;
    private String currentSongTitle;
    private String currentSongArtist;
    private Uri currentAlbumArtUri;
    private boolean isPlaying = false;
    private Handler handler;
    private List<OnPlaybackChangeListener> listeners;

    public interface OnPlaybackChangeListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressChanged(int progress, int duration);
        void onSongChanged(String songPath);
    }

    public interface OnPlaybackControlListener extends OnPlaybackChangeListener {
        void onNext();
        void onPrevious();
    }

    private MediaPlayerManager() {
        mediaPlayer = new MediaPlayer();
        handler = new Handler(Looper.getMainLooper());
        listeners = new ArrayList<>();

        mediaPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            notifyPlaybackStateChanged();
        });
    }

    public static synchronized MediaPlayerManager getInstance() {
        if (instance == null) {
            instance = new MediaPlayerManager();
        }
        return instance;
    }

    public void addListener(OnPlaybackChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // Update the new listener with current state
            if (currentSongPath != null) {
                listener.onSongChanged(currentSongPath);
                listener.onPlaybackStateChanged(isPlaying);
                if (mediaPlayer != null) {
                    listener.onProgressChanged(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                }
            }
        }
    }

    public void removeListener(OnPlaybackChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlaybackStateChanged() {
        for (OnPlaybackChangeListener listener : listeners) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }

    private void notifyProgressChanged() {
        if (mediaPlayer != null) {
            for (OnPlaybackChangeListener listener : listeners) {
                listener.onProgressChanged(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
            }
        }
    }

    private void notifySongChanged() {
        for (OnPlaybackChangeListener listener : listeners) {
            listener.onSongChanged(currentSongPath);
        }
    }

    public void playSong(String path, String title, String artist, Uri albumArtUri) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            this.currentSongPath = path;
            this.currentSongTitle = title;
            this.currentSongArtist = artist;
            this.currentAlbumArtUri = albumArtUri;
            this.isPlaying = true;
            
            notifyPlaybackStateChanged();
            notifySongChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.pause();
                stopProgressUpdates();
            } else {
                mediaPlayer.start();
                startProgressUpdates();
            }
            isPlaying = !isPlaying;
            notifyPlaybackStateChanged();
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            notifyProgressChanged();
        }
    }

    private void startProgressUpdates() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    notifyProgressChanged();
                    handler.postDelayed(this, 100);
                }
            }
        });
    }

    private void stopProgressUpdates() {
        handler.removeCallbacksAndMessages(null);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getCurrentSongPath() {
        return currentSongPath;
    }

    public String getCurrentSongTitle() {
        return currentSongTitle;
    }

    public String getCurrentSongArtist() {
        return currentSongArtist;
    }

    public Uri getCurrentAlbumArtUri() {
        return currentAlbumArtUri;
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
        listeners.clear();
        instance = null;
    }

    public void playNext() {
        // Notify listeners to handle next song
        for (OnPlaybackChangeListener listener : listeners) {
            if (listener instanceof OnPlaybackControlListener) {
                ((OnPlaybackControlListener) listener).onNext();
            }
        }
    }

    public void playPrevious() {
        // Notify listeners to handle previous song
        for (OnPlaybackChangeListener listener : listeners) {
            if (listener instanceof OnPlaybackControlListener) {
                ((OnPlaybackControlListener) listener).onPrevious();
            }
        }
    }
} 