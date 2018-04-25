package com.dftc.debug.serv.req;

import com.dftc.debug.Constants.Config;
import com.dftc.debug.serv.support.HttpPostParser;
import com.dftc.debug.serv.support.Progress;
import com.dftc.debug.utils.LogUtils;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.util.Map;

public class HttpProgressHandler implements HttpRequestHandler {

    static final String TAG = "HttpProgressHandler";

    public HttpProgressHandler() {
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        HttpPostParser parser = new HttpPostParser();
        Map<String, String> params = parser.parse(request);
        String id = params.get("id");
        LogUtils.d("handle id:" + id);
        if (id == null) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        String progress = Progress.get(id) + "";
        LogUtils.dLog(TAG, id + ": " + progress);
        response.setEntity(new StringEntity(progress, Config.ENCODING));
    }

}
