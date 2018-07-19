package com.base12innovations.android.fireroad;

import android.os.Handler;
import android.os.Looper;

public class TaskDispatcher {

    public interface Task <E> {
        E perform();
    }

    public interface TaskNoReturn {
        void perform();
    }

    public interface CompletionBlock <E> {
        void completed(E arg);
    }

    public static <T> void perform(final Task<T> task, final CompletionBlock<T> completion) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final T result = task.perform();

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        completion.completed(result);
                    }
                });
            }
        }) .start();
    }

    public static void inBackground(final TaskNoReturn task) {
        inBackground(task, false);
    }

    public static void inBackground(final TaskNoReturn task, boolean sync) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                task.perform();
            }
        });
        thread.start();
        if (sync) {
            try {
                thread.join();
            } catch (InterruptedException e) { }
        }
    }

    public static void onMain(final TaskNoReturn task) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                task.perform();
            }
        });
    }


}
