package com.liquidware.cloudview;

public interface Event {
    public void onTimerTick(long millisUpTime);
    public void onRemoteAssetUpdated(int id, Object object);
}
