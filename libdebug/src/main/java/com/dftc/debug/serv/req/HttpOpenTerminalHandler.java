package com.dftc.debug.serv.req;

import android.content.Context;

import com.dftc.debug.Constants.Config;
import com.dftc.debug.utils.LogUtils;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;

/**
 * 打开终端请求处理
 * <p>
 * Created by xuqiqiang on 2017/04/17.
 */
public class HttpOpenTerminalHandler implements HttpRequestHandler {

    static final String TAG = "HttpOpenTerminalHandler";
    private Context mContext;

    public HttpOpenTerminalHandler(Context context) {
        this.mContext = context;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        LogUtils.d("handle");
        String result = DatabaseHandler.getInstance(mContext).openTerminalResponse();
        response.setEntity(new StringEntity(result, Config.ENCODING));
    }

}