package cn.ctkqiang.huaxiahongke.utilities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class NetworkUtils
{
    @SuppressLint("MissingPermission")
    public static String getCurrentSSID(Context context)
    {
        if (context == null) return null;

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            {
                return null;
            }
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) return null;

        String ssid = wifiInfo.getSSID();
        if (ssid.equals("<unknown ssid>") || ssid.equals("0x")) return null;

        return ssid.replace("\"", "");
    }

    public static boolean isNetworkConnected(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @SuppressLint("DefaultLocale")
    public static String generateMulticastIP(String ssid)
    {
        String fallbackIP = "239.255.255.250";
        if (ssid == null || ssid.isEmpty()) return fallbackIP;

        try
        {
            CRC32 crc = new CRC32();
            crc.update(ssid.getBytes(StandardCharsets.UTF_8));
            long hash = crc.getValue();

            return String.format("239.%d.%d.%d",
                    (hash >> 16) & 0xFF,
                    (hash >> 8) & 0xFF,
                    hash & 0xFF);
        } catch (Exception e)
        {
            return fallbackIP;
        }
    }
}