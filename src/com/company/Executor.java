package com.company;

import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Executor extends ThreadPoolExecutor {
    private boolean isPaused;
    private ReentrantLock lock;
    private Condition condition;
    private WorkerQueue workQueue;

    public Executor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, WorkerQueue workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue.queue);
        this.workQueue = workQueue;
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTaskJob<T>(runnable, value);
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable runnable) {
        super.beforeExecute(thread, runnable);
        lock.lock();

        try {
            while (isPaused) condition.await();
        } catch (InterruptedException ie) {
            thread.interrupt();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        workQueue.executeLinked(((FutureTaskJob) runnable).getTask().toString());
    }

    public boolean isRunning() {
        return !isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void pause() {
        lock.lock();
        try {
            isPaused = true;
        } finally {
            lock.unlock();
        }
    }

    public void resume() {
        lock.lock();
        try {
            isPaused = false;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
