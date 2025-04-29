package cn.ctkqiang.huaxiahongke.utilities;

import static android.content.Context.AUDIO_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import cn.ctkqiang.huaxiahongke.constants.Constants;

public class AudioPlayer {
    private static final String TAG = Constants.TAG_NAME;
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioTrack audioTrack;
    private volatile boolean isInitialized = false;
    private final Object lock = new Object();

    private AudioManager audioManager;

    @SuppressLint("NewApi")
    public AudioPlayer(Context context) {
        audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        audioManager.requestAudioFocus(focusListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    private AudioManager.OnAudioFocusChangeListener focusListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            cleanup();
        }
    };


    private void initializeAudioTrack() {
        new Thread(() -> {
            try {
                int bufferSize = AudioTrack.getMinBufferSize(
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT
                );

                synchronized (lock) {
                    audioTrack = new AudioTrack(
                            AudioManager.STREAM_VOICE_CALL,
                            SAMPLE_RATE,
                            CHANNEL_CONFIG,
                            AUDIO_FORMAT,
                            bufferSize,
                            AudioTrack.MODE_STREAM
                    );

                    if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                        audioTrack.play();
                        isInitialized = true;
                        Log.d(TAG, "AudioTrack initialized successfully");
                    } else {
                        Log.e(TAG, "AudioTrack initialization failed");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "AudioTrack creation error: " + e.getMessage());
                cleanup();
            }
        }).start();
    }

    public void playAudio(byte[] data, int length) {
        synchronized (lock) {
            if (!isInitialized || audioTrack == null) {
                Log.w(TAG, "AudioTrack not ready, skipping playback");
                return;
            }

            try {
                audioTrack.write(data, 0, length);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Playback error: " + e.getMessage());
                cleanup();
            }
        }
    }

    public void cleanup() {
        synchronized (lock) {
            if (audioTrack != null) {
                try {
                    if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop();
                    }
                    audioTrack.release();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Cleanup error: " + e.getMessage());
                }
                audioTrack = null;
                isInitialized = false;
            }
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}