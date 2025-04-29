package cn.ctkqiang.huaxiahongke.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import cn.ctkqiang.huaxiahongke.R;
import cn.ctkqiang.huaxiahongke.constants.Constants;

public class BluetoothTerminal extends AppCompatActivity
{

    private static final String TAG = Constants.TAG_NAME;

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private TextView terminalOutput;
    private EditText commandInput;
    private ScrollView scrollView;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_bluetooth_terminal);

        Objects.requireNonNull(this.getSupportActionBar()).hide(); // 隐藏ActionBar

        this.terminalOutput = this.findViewById(R.id.terminalOutput);
        this.commandInput = this.findViewById(R.id.commandInput);
        this.scrollView = this.findViewById(R.id.scrollView);
        this.sendButton = this.findViewById(R.id.sendButton);

        this.sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                BluetoothTerminal.this.sendCommand();
            }
        });

        this.commandInput.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN))
                {
                    BluetoothTerminal.this.sendCommand();
                    return true;
                }
                return false;
            }
        });
    }

    private void sendCommand()
    {
        final String command = this.commandInput.getText().toString().trim();
        if (!TextUtils.isEmpty(command))
        {
            this.appendOutput(">> " + command);
            this.commandInput.setText("");
            this.sendBluetoothData(command);
        }
    }

    private void sendBluetoothData(String command)
    {
        if (bluetoothSocket != null && bluetoothSocket.isConnected())
        {
            try
            {
                outputStream.write(command.getBytes());
                outputStream.flush();
                appendOutput("命令已发送: " + command);
            } catch (IOException e)
            {
                e.printStackTrace();
                appendOutput("发送命令时出错: " + e.getMessage());
            }
        } else
        {
            appendOutput("蓝牙未连接。");
        }
    }

    private void appendOutput(String message)
    {
        this.terminalOutput.append(message + "\n");
        this.scrollView.post(new Runnable()
        {
            @Override
            public void run()
            {
                BluetoothTerminal.this.scrollView.fullScroll(android.view.View.FOCUS_DOWN);
            }
        });
    }
}
