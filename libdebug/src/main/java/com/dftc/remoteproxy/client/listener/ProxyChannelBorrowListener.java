package com.dftc.remoteproxy.client.listener;

import io.netty.channel.Channel;

public interface ProxyChannelBorrowListener {

    void success(Channel channel);

    void error(Throwable cause);

}
