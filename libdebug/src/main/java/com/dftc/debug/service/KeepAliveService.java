/*
 *
 *  *    Copyright (C) 2016 xuqiqiang
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */
package com.dftc.debug.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.dftc.debug.Constants;
import com.dftc.debug.serv.WebServer;
import com.dftc.debug.serv.req.DatabaseHandler;
import com.dftc.debug.serv.req.database.NetworkUtils;
import com.dftc.debug.utils.CommonUtil;
import com.dftc.debug.utils.LogUtils;
import com.dftc.debug.utils.Utils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 后台保活Service
 * <p>
 * Created by xuqiqiang on 2017/04/17.
 */
public class KeepAliveService extends Service implements WebServer.OnWebServListener {

    public static final String KEY_CUSTOM_DATABASE = "customDatabaseFiles";
    public static final String KEY_STATUS = "status";
    public static final String KEY_SET_MAC = "setMac";

    public static final int STATUS_IDLE = 0, STATUS_START = 1, STATUS_CLOSE = 2;
    private static final String TAG = KeepAliveService.class.getSimpleName();
    /**
     * 错误时自动恢复的次数。如果仍旧异常，则继续传递。
     */
    private static final int RESUME_COUNT = 3;
    /**
     * 错误时重置次数的时间间隔。
     */
    private static final int RESET_INTERVAL = 3000;
    private static final Object mWebServerLock = new Object();
    private static final Object mRemoteProxyLock = new Object();
    public static int mStatus = STATUS_IDLE;
    private int errCount = 0;
    private Timer mTimer = new Timer(true);
    private TimerTask resetTask;
    private int mPort;
    private WebServer webServer;
    private boolean isRemoteProxyRunning;

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.initialize(this);
        LogUtils.dLog(TAG, "onCreate");
    }

    private void openWebServer() {
        new Thread() {
            public void run() {
                synchronized (mWebServerLock) {
                    if (webServer == null) {
                        mPort = CommonUtil.getSingleton().getEnablePort(Constants.Config.PORT);
                        SharedPreferences sharedPreferences = getSharedPreferences(
                                Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                        sharedPreferences.edit().putInt(Constants.SHARED_PREF_KEY_PORT, mPort).apply();
                        Log.d("libdebug", NetworkUtils.getAddressLog(KeepAliveService.this, mPort));
                        webServer = new WebServer(KeepAliveService.this, mPort, Constants.Config.WEBROOT);
                        webServer.setOnWebServListener(KeepAliveService.this);
                        webServer.setDaemon(true);
                        webServer.start();
                    }
                }
            }
        }.start();
    }

    private void closeWebServer() {
        synchronized (mWebServerLock) {
            if (webServer != null) {
                webServer.close();
                webServer = null;
            }
        }
    }

    private void initRemoteProxy() {
        synchronized (mRemoteProxyLock) {
            if (isRemoteProxyRunning)
                return;
            try {
                Class<?> containerHelper = Class.forName("com.dftc.remoteproxy.ContainerHelper");
                Class[] argTypes = new Class[]{Context.class, String.class};
                Method start = containerHelper.getMethod("start", argTypes);
                isRemoteProxyRunning = (boolean) start.invoke(null, this,
                        Utils.getAddressMAC(this).replace(":", ""));
            } catch (Exception e) {
                LogUtils.e(e, "initRemoteProxy");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        LogUtils.dLog(TAG, "onStartCommand");

        if (intent != null) {
            String mac = intent.getStringExtra(KEY_SET_MAC);
            if (!TextUtils.isEmpty(mac)) {
                getSharedPreferences(
                        Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putString(Constants.SHARED_PREF_KEY_MAC, mac).apply();
            }
            int status = intent.getIntExtra(KEY_STATUS, STATUS_IDLE);
            if (status == STATUS_START) {
                mStatus = STATUS_START;
            } else if (status == STATUS_CLOSE) {
                mStatus = STATUS_CLOSE;
                stop();
                return START_NOT_STICKY;
            } else if (status == STATUS_IDLE) {
                if (mStatus == STATUS_CLOSE) {
                    stop();
                    return START_NOT_STICKY;
                } else {
                    mStatus = STATUS_START;
                }
            }
            Serializable serializable = intent.getSerializableExtra(KEY_CUSTOM_DATABASE);
            if (serializable != null && serializable instanceof HashMap) {
                LogUtils.d("setCustomDatabaseFiles");
                DatabaseHandler.getInstance(this)
                        .setCustomDatabaseFiles((HashMap) serializable);
            }
        }

        if (webServer == null)
            openWebServer();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                LogUtils.d("ContainerHelper.start()");
                initRemoteProxy();
            }
        }, 3000);


        int tenMinutes = 3 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + tenMinutes;
        Intent i = new Intent(this, KeepAliveService.class);
        i.putExtra(KEY_STATUS, STATUS_IDLE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (manager != null)
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return Service.START_STICKY;
    }

    private void stop() {
        Intent i = new Intent(this, KeepAliveService.class);
        i.putExtra(KEY_STATUS, STATUS_IDLE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (manager != null)
            manager.cancel(pi);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeWebServer();

        synchronized (mRemoteProxyLock) {
            try {
                Class<?> containerHelper = Class.forName("com.dftc.remoteproxy.ContainerHelper");
                Method stop = containerHelper.getMethod("stop");
                stop.invoke(null);
                isRemoteProxyRunning = false;
            } catch (Exception e) {
                LogUtils.e(e, "initRemoteProxy");
            }
        }

        LogUtils.dLog(TAG, "onDestroy");
    }

    @Override
    public void onStarted() {
        LogUtils.d("onStarted");
    }

    @Override
    public void onStopped() {
        LogUtils.d("onStopped");
    }

    @Override
    public void onError(int code) {
        LogUtils.d("onError:" + code);
        closeWebServer();
        errCount++;
        restartResetTask(RESET_INTERVAL);
        if (errCount <= RESUME_COUNT) {
            LogUtils.d("Retry times: " + errCount);
            openWebServer();
        } else {
            errCount = 0;
            cancelResetTask();
        }
    }

    private void cancelResetTask() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
    }

    private void restartResetTask(long delay) {
        cancelResetTask();
        resetTask = new TimerTask() {
            @Override
            public void run() {
                errCount = 0;
                resetTask = null;
                LogUtils.d("ResetTask executed.");
            }
        };
        mTimer.schedule(resetTask, delay);
    }
}
