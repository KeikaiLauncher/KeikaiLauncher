/*
 * Copyright 2015-2017 Hayai Software
 * Copyright 2018 The KeikaiLauncher Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anpmech.launcher.threading;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SimpleTaskConsumerManager {

    private static final String INTERRUPTED_MSG = "Task was interrupted.";

    private static final String TAG = "SimpleTaskManager";

    private final BlockingQueue<Task> mTasks;

    private final Thread[] mThreads;

    private boolean mSubmissionsAccepted = true;

    public SimpleTaskConsumerManager(final int numConsumers, final int queueSize) {
        if (queueSize < 1) {
            mTasks = new LinkedBlockingQueue<>();
        } else {
            mTasks = new ArrayBlockingQueue<>(queueSize);
        }

        mThreads = new Thread[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            mThreads[i] = new Thread(new SimpleTaskConsumer(mTasks));
            mThreads[i].start();
        }
    }

    public SimpleTaskConsumerManager(final int numConsumers) {
        this(numConsumers, 0);
    }

    private static void putTask(final BlockingQueue<Task> tasks, final Task task) {
        try {
            tasks.put(task);
        } catch (final InterruptedException e) {
            Log.e(TAG, INTERRUPTED_MSG, e);
        }
    }

    public void addTask(final Task task) {
        if (mSubmissionsAccepted) {
            putTask(mTasks, task);
        }
    }

    private void blockThreadsUntilFinished() {
        for (final Thread thread : mThreads) {
            try {
                thread.join();
            } catch (final InterruptedException e) {
                Log.e(TAG, INTERRUPTED_MSG, e);
            }
        }
    }

    public void destroyAllConsumers(final boolean finishCurrentTasks,
            final boolean blockUntilFinished) {
        if (mSubmissionsAccepted) {
            mSubmissionsAccepted = false;

            if (finishCurrentTasks) {
                final Task dieTask = new DieTask();
                for (final Thread mThread : mThreads) {
                    putTask(mTasks, dieTask);
                }

                if (blockUntilFinished) {
                    blockThreadsUntilFinished();
                }
            } else {
                removeAllTasks();
            }
        }
    }

    public void destroyAllConsumers(final boolean finishCurrentTasks) {
        destroyAllConsumers(finishCurrentTasks, false);
    }

    @Override
    protected void finalize() throws Throwable {
        //make sure the threads are properly killed
        destroyAllConsumers(false);

        super.finalize();
    }

    private void removeAllTasks() {
        for (final Thread thread : mThreads) {
            thread.interrupt();
        }
        mTasks.clear();
    }

    public interface Task {

        //Returns true if you want the thread that run the task to continue running.
        boolean doTask();
    }

    //Dummy task, does nothing. Used to properly wake the threads to kill them.
    private static final class DieTask implements Task {

        @Override
        public boolean doTask() {
            return false;
        }
    }

    private static final class SimpleTaskConsumer implements Runnable {

        private final BlockingQueue<Task> mTasks;

        private SimpleTaskConsumer(final BlockingQueue<Task> tasks) {
            mTasks = tasks;
        }

        @Override
        public void run() {
            Task task = null;

            do {
                try {
                    task = mTasks.take();
                } catch (final InterruptedException e) {
                    // Interruption is part of the lifecycle here.
                    Log.v(TAG, INTERRUPTED_MSG, e);
                }
            } while (task == null || task.doTask());
        }
    }
}
