package com.interra.microphonetest;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gauravk.audiovisualizer.visualizer.BarVisualizer;

import java.util.logging.Handler;

public class PlayVolume {
    ImageButton btn_play, btn_reset, btn_pause;
    ImageView imgUp, imgDown, imgMute;
    BarVisualizer visualizer;
    Context context;
    MediaPlayer mediaPlayer;
    public static int restId;
    AudioManager audioManager;
    static int curVolume;

    PlayVolume(Context context) {
        this.context = context;
    }

    public void init() {
        btn_play = ((Activity) context).findViewById(R.id.btn_play);
        btn_pause = ((Activity) context).findViewById(R.id.btn_pause);
        btn_reset = ((Activity) context).findViewById(R.id.btn_reset);
        visualizer = ((Activity) context).findViewById(R.id.visualizer);
        imgDown = ((Activity) context).findViewById(R.id.img_down);
        imgUp = ((Activity) context).findViewById(R.id.img_up);
        imgMute = ((Activity) context).findViewById(R.id.img_mute);

        audioManager = (AudioManager) ((Activity) context).getSystemService(Context.AUDIO_SERVICE);
        curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        click();

    }

    private void click() {
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayerStart();
            }
        });
        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
            }
        });
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    visualizer.release();
                    mediaPlayer = null;
                    mediaPlayerStart();
                }
            }
        });
        imgDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundValue(-2);
            }
        });
        imgUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundValue(+2);
            }
        });
        imgMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundValue(0);
            }
        });
    }

    private void soundValue(int volume) {
        curVolume = curVolume + volume;
        if (curVolume >= 100) {
            curVolume = 100;
        } else if (volume == 0 || curVolume < 0) {
            curVolume = 0;
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, 0);
        Toast.makeText(context, "Volume: " + curVolume, Toast.LENGTH_SHORT).show();
    }

    public void mediaPlayerReset() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            visualizer.release();
            mediaPlayer = null;
        }
    }

    public void mediaPlayerStart() {
        if (mediaPlayer == null) {

            mediaPlayer = MediaPlayer.create(context, restId);
            int audiosessionId = mediaPlayer.getAudioSessionId();
            if (audiosessionId != -1) {
                visualizer.setAudioSessionId(audiosessionId);
            }
        }
        mediaPlayer.start();
    }
}
