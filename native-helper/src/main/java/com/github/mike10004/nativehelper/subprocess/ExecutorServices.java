package com.github.mike10004.nativehelper.subprocess;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;
import java.util.function.Supplier;

class ExecutorServices {

    private ExecutorServices() {}

    public static Supplier<ListeningExecutorService> newSingleThreadExecutorServiceFactory() {
        return () -> MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }
}
