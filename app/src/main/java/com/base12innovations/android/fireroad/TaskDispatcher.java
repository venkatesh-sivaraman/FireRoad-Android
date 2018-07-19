package com.base12innovations.android.fireroad;

import android.os.Handler;
import android.os.Looper;

public class TaskDispatcher {

    public interface Task <E> {
        E perform();
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
}
