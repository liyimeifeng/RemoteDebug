package com.dftc.debug.receiver;

public interface OnStorageListener {

    /**
     * 挂载
     */
    void onMounted();

    /**
     * 未挂载
     */
    void onUnmounted();

}
