package com.marabytes.musicplayernew;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private MediaPlayerManager mediaPlayerManager;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayerManager = MediaPlayerManager.getInstance();
        notificationManager = NotificationManagerCompat.from(this);
        
        // Create notification channel for Android O and above
        createNotificationChannel();
        
        // Initialize MediaSession
        mediaSession = new MediaSessionCompat(this, "MusicPlayerSession");
        mediaSession.setActive(true);
        
        // Add listener to update notification when playback changes
        mediaPlayerManager.addListener(new MediaPlayerManager.OnPlaybackChangeListener() {
            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
                updateNotification();
            }

            @Override
            public void onProgressChanged(int progress, int duration) {
                // Not needed for notification
            }

            @Override
            public void onSongChanged(String songPath) {
                updateNotification();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW); // LOW importance prevents sound
            channel.setDescription("Controls for the music player");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        // Create pending intent for opening the app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Create action buttons
        NotificationCompat.Action prevAction = new NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Previous",
            createActionIntent("PREVIOUS"));

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
            mediaPlayerManager.isPlaying() ? 
                android.R.drawable.ic_media_pause : 
                android.R.drawable.ic_media_play,
            mediaPlayerManager.isPlaying() ? "Pause" : "Play",
            createActionIntent("PLAY_PAUSE"));

        NotificationCompat.Action nextAction = new NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Next",
            createActionIntent("NEXT"));

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(mediaPlayerManager.getCurrentSongTitle())
            .setContentText(mediaPlayerManager.getCurrentSongArtist())
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction);

        // Load album art asynchronously
        try {
            Uri albumArtUri = mediaPlayerManager.getCurrentAlbumArtUri();
            if (albumArtUri != null) {
                Bitmap albumArt = Glide.with(this)
                    .asBitmap()
                    .load(albumArtUri)
                    .submit()
                    .get();
                builder.setLargeIcon(albumArt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Notification notification = builder.build();
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, notification);
    }

    private PendingIntent createActionIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PLAY_PAUSE":
                    mediaPlayerManager.togglePlayPause();
                    break;
                case "NEXT":
                    mediaPlayerManager.playNext();
                    break;
                case "PREVIOUS":
                    mediaPlayerManager.playPrevious();
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
        stopForeground(true);
    }
} 