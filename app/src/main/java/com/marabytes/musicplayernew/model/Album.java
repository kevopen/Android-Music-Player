package com.marabytes.musicplayernew.model;

public class Album {
    private String title;
    private String artist;
    private String albumArt;
    private String id;

    public Album(String title, String artist, String albumArt, String id) {
        this.title = title;
        this.artist = artist;
        this.albumArt = albumArt;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    public String getId() {
        return id;
    }
} 