package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


interface ITask {
    public void execute();
}

public class Main {
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            FileHandler fh = new FileHandler("./task.log");
            fh.setFormatter(new SimpleFormatter());
            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        WorkerQueue wq = new WorkerQueue();
        wq.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$>");
            String c = scanner.nextLine();
            String[] command = c.split(" ");
            switch (command[0]) {
                case "e":
                case "exit":
                    wq.shutdown();
                    break;

                case "p":
                case "pause":
                    wq.pause();
                    break;

                case "re":
                case "resume":
                    wq.resume();
                    break;

                case "cr":
                case "create":
                    if (command.length <= 1) return;
                    System.out.println("removed: " + wq.removeTask(command[1]));
                    wq.create(command[1].split("::"));
                    break;

                case "rm":
                case "remove":
                    if (command.length <= 1) return;
                    System.out.println("removed: " + wq.removeTask(command[1]));
                    break;

                default:
                    System.out.println("?");
            }

        }
    }
}

class WorkerQueue {
    /* source task list*/
    public String TasklistSrc = "tasks.txt";
    /* The maximum size of the pool */
    public int MaxPool = 10;
    /* The amount of time a single task alive in seconds */
    public int MaxTaskLiveTime = 20;

    protected Executor executor;
    protected LinkedBlockingQueue<Runnable> queue;
    public ConcurrentHashMap<String, Future> futureMap;
    public ConcurrentHashMap<String, String> queueLinked;
    public ConcurrentHashMap<String, String> queueLinkedAll;

    public void start() {
        int processors = Runtime.getRuntime().availableProcessors();
        queue = new LinkedBlockingQueue<Runnable>();

        futureMap = new ConcurrentHashMap<>();
        queueLinked = new ConcurrentHashMap<>();
        queueLinkedAll = new ConcurrentHashMap<>();

        executor = new Executor(processors, MaxPool, MaxTaskLiveTime, TimeUnit.SECONDS, this);
        for (String[] li : this.getTasksList()) {
            create(li);
        }
    }

    public String create(String[] li) {
        TaskJob task = new TaskJob(li[0]);
        System.out.printf("Scheduling ID:%s  file-link:%s\n", task.ID, task.SrcFile);

        queueLinkedAll.put(li[0], task.ID);
        if (li.length > 1) {
            if (queueLinkedAll.get(li[1]) != null) {
                queueLinked.put(queueLinkedAll.get(li[1]), task.ID);
                queueLinked.put(task.ID, queueLinkedAll.get(li[1]));
            }
        }

        Future rf = executor.submit(task);
        futureMap.put(task.ID, rf);
        return task.ID;
    }

    public void shutdown() {
        executor.shutdown();
        System.exit(0);
    }

    public boolean removeTask(String id) {
        FutureTaskJob t = (FutureTaskJob) futureMap.get(id);
        if (t != null) {
            boolean re = t.cancel(false);
            re = executor.remove(t);
            futureMap.remove(id);
            queueLinked.remove(id);
            return re;
        }
        return false;
    }

    public void removeExecuteLink(String id) {
        queueLinked.remove(queueLinked.get(id));
        queueLinked.remove(id);
    }

    public void executeLinked(String rid) {
        String link = queueLinked.get(rid);
        if (link != null) {
            Future fr = futureMap.get(link);
            if (fr != null && !fr.isDone()) {
                executor.execute((Runnable) fr);
                this.removeExecuteLink(rid);
            }
        }

        futureMap.remove(rid);
    }

    public void pause() {
        executor.pause();
    }

    public void resume() {
        executor.resume();
    }

    public void clear() {
        queue.clear();
    }

    private LinkedList<String[]> getTasksList() {
        LinkedList li = new LinkedList();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(System.getProperty("user.dir"), this.TasklistSrc)))) {
            String linLink;

            while ((linLink = br.readLine()) != null) {
                String[] srcLink = linLink.split("::");
                li.add(srcLink);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return li;
    }
}
