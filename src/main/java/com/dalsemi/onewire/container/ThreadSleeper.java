package com.dalsemi.onewire.container;

public class ThreadSleeper implements Sleeper{
    @Override
    public void sleep(Integer ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}
