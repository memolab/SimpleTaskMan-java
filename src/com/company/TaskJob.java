package com.company;

import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;


interface ITask {
    public void execute();
}

public class TaskJob implements Runnable, ITask {
    public final String ID;
    public final String SrcFile;

    public TaskJob(String srcFile) {
        this.ID = UUID.randomUUID().toString();
        this.SrcFile = srcFile;
    }

    @Override
    public void run() {
        this.execute();
    }

    @Override
    public void execute() {
        int randomNum = 2 + (int) (Math.random() * 10);
        Main.LOGGER.info(String.format("Execute: ID:%s, %s, sleep..%ss", ID, SrcFile, randomNum));
        try {
            TimeUnit.SECONDS.sleep(randomNum);
        } catch (InterruptedException e) {
            Main.LOGGER.warning(String.format("\ntask interrupted %s\n", ID));
        }
    }

    @Override
    public String toString() {
        return this.ID;
    }
}

class FutureTaskJob<V> extends FutureTask<V> {

    private Runnable task;

    public FutureTaskJob(Runnable runnable, V result) {
        super(runnable, result);
        this.task = runnable;
    }

    public Runnable getTask() {
        return this.task;
    }
}