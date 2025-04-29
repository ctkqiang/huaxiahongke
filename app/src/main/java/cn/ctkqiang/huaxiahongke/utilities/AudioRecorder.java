package cn.ctkqiang.huaxiahongke.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

public class AudioRecorder
{
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private volatile boolean isRecording = false;

    private final AudioRecord audioRecord;
    private final MulticastManager multicastManager;

    public boolean isRecording() {
        return isRecording;
    }

    public AudioRecorder(Activity context, MulticastManager multicastManager) throws IllegalStateException
    {
        this.multicastManager = multicastManager;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            throw new IllegalStateException("未授予麦克风权限，请在设置中开启录音权限。");
        }

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE)
        {
            throw new IllegalStateException("获取缓冲区大小失败，设备可能不支持该采样率。");
        }

        this.audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (this.audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
        {
            throw new IllegalStateException("音频录制器初始化失败，请检查设备兼容性。");
        }
    }

    public void startRecording()
    {
        if (this.audioRecord.getState() != AudioRecord.STATE_INITIALIZED) return;

        this.isRecording = true;
        this.audioRecord.startRecording();
        new Thread(this::recordAudio, "音频录制线程").start();
    }

    private void recordAudio()
    {
        byte[] buffer = new byte[1024];
        while (this.isRecording)
        {
            int bytesRead = this.audioRecord.read(buffer, 0, buffer.length);
            if (bytesRead > 0)
            {
                this.multicastManager.send(buffer);
            }
        }
    }

    public void stopRecording()
    {
        if (!this.isRecording) return;
        this.isRecording = false;
        this.audioRecord.stop();
    }

    public void cleanup()
    {
        if (this.audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
        {
            this.audioRecord.release();
        }
    }
}
