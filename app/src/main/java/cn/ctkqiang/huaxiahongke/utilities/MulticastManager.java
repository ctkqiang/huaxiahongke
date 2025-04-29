package cn.ctkqiang.huaxiahongke.utilities;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;

import cn.ctkqiang.huaxiahongke.constants.Constants;

public class MulticastManager
{
    private static final String TAG = Constants.TAG_NAME;
    private static final int PORT = 5007;
    private static final int BUFFER_SIZE = 1024;

    private MulticastSocket socket;
    private InetAddress group;
    private boolean isInitialized = false;
    private NetworkInterface networkInterface;

    public MulticastManager(String groupIP) throws IOException {
        try {
            // 自动选择可用网络接口
            NetworkInterface networkInterface = findActiveNetworkInterface();

            socket = new MulticastSocket(PORT);
            socket.setReuseAddress(true);
            socket.setNetworkInterface(networkInterface);

            group = InetAddress.getByName(groupIP);
            socket.joinGroup(new InetSocketAddress(group, PORT), networkInterface);

            isInitialized = true;
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private NetworkInterface findActiveNetworkInterface() throws IOException {
        ArrayList<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface iface : interfaces) {
            if (iface.isUp() && !iface.isLoopback()) {
                return iface;
            }
        }
        throw new IOException("No active network interface found");
    }

    private NetworkInterface findNetworkInterface() throws IOException
    {
        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces()))
        {
            if (iface.isUp() && !iface.isLoopback())
            {
                Log.d(TAG, "Using network interface: " + iface.getName());
                return iface;
            }
        }
        throw new IOException("No suitable network interface found");
    }

    public void send(byte[] audioData)
    {
        if (!isInitialized)
        {
            Log.e(TAG, "Cannot send - multicast not initialized");
            return;
        }

        new Thread(() ->
        {
            try
            {
                DatagramPacket packet = new DatagramPacket(
                        audioData,
                        audioData.length,
                        new java.net.InetSocketAddress(group, PORT)
                );
                socket.send(packet);
            } catch (IOException e)
            {
                Log.e(TAG, "Send error: " + e.getMessage());
            }
        }).start();
    }

    public void startReceiving(PacketReceiver receiver)
    {
        if (!isInitialized)
        {
            Log.e(TAG, "Cannot receive - multicast not initialized");
            return;
        }

        new Thread(() ->
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!socket.isClosed())
            {
                try
                {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    receiver.onPacketReceived(packet.getData(), packet.getLength());
                } catch (IOException e)
                {
                    if (!socket.isClosed())
                    {
                        Log.e(TAG, "Receive error: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    public void close()
    {
        if (socket != null && !socket.isClosed())
        {
            try
            {
                if (group != null && networkInterface != null)
                {
                    socket.leaveGroup(new java.net.InetSocketAddress(group, PORT), networkInterface);
                }
                socket.close();
                Log.i(TAG, "Multicast closed successfully");
            } catch (IOException e)
            {
                Log.e(TAG, "Close error: " + e.getMessage());
            }
        }
        isInitialized = false;
    }

    public boolean isInitialized()
    {
        return isInitialized;
    }

    public interface PacketReceiver
    {
        void onPacketReceived(byte[] data, int length);
    }
}