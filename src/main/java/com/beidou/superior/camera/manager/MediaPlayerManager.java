package com.beidou.superior.camera.manager;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import java.io.IOException;

public class MediaPlayerManager {
    private MediaPlayer mPlayer;
    private int rawID;
    private Context context;
    private AssetFileDescriptor fileDescriptor;

    public MediaPlayerManager(Context context, int rawID) {
        this.context = context;
        this.rawID = rawID;
        initMediaPlayer();
    }

    public void setRawID(int rawID) {
        this.rawID = rawID;
        resetDataResource();
    }

    private void initMediaPlayer() {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int ringerMode = am.getRingerMode();
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                mPlayer = new MediaPlayer();
                resetDataResource();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resetDataResource() {
        fileDescriptor = context.getResources().openRawResourceFd(rawID);
        try {
            mPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            fileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
            mPlayer.reset();
        }
    }

    private void prepareMedia() {
        if (mPlayer == null)
            return;
        try {
            mPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playerMedia() {
        if (mPlayer == null)
            return;
        if (!mPlayer.isPlaying()) {
            prepareMedia();
            mPlayer.start();
        }
    }

    public void albumMedia() {
        Uri systemAudioUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone rt = RingtoneManager.getRingtone(context, systemAudioUri);
        rt.play();
    }

    public  void stopMedia() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
            mPlayer.release();
        }
        mPlayer = null;
    }

}
