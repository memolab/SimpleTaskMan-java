package com.company;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


public class WorkerQueueTest {
    WorkerQueue wq;

    @Before
    public void setUp() throws Exception {
        wq = new WorkerQueue();
        wq.start();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void cleanupHelperMaps() throws Exception {
        long count = 0;
        while (count < wq.executor.getTaskCount()){
            try {
                TimeUnit.SECONDS.sleep(5);
                count = wq.executor.getCompletedTaskCount();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertEquals("futureMap length must be 0", 0, wq.futureMap.size());
        assertEquals("queueLinked length must be 0", 0, wq.queueLinked.size());
    }

}