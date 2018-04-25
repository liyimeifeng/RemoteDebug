package com.dftc.debug;

import android.content.Context;
import android.content.Intent;

import com.dftc.debug.service.KeepAliveService;

import static com.dftc.debug.service.KeepAliveService.KEY_SET_MAC;
import static com.dftc.debug.service.KeepAliveService.KEY_STATUS;
import static com.dftc.debug.service.KeepAliveService.STATUS_CLOSE;
import static com.dftc.debug.service.KeepAliveService.STATUS_START;

/**
 * Created by xuqiqiang on 2018/4/16.
 */
public class DebugHelper {

    public static void start(Context context) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.putExtra(KEY_STATUS, STATUS_START);
        context.startService(intent);
    }

    public static void start(Context context, String mac) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.putExtra(KEY_STATUS, STATUS_START);
        intent.putExtra(KEY_SET_MAC, mac);
        context.startService(intent);
    }

    public static void close(Context context) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.putExtra(KEY_STATUS, STATUS_CLOSE);
        context.startService(intent);
    }
}
