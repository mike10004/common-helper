package com.github.mike10004.nativehelper.subprocess;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;

class ExecutorServices {

    private ExecutorServices() {}

    private static final AtomicInteger counter = new AtomicInteger(0);

    public static Supplier<ListeningExecutorService> newSingleThreadExecutorServiceFactory(String poolName) {
        return () -> MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(new NamedThreadFactory(poolName)));
    }

    private static class NamedThreadFactory implements ThreadFactory {

        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final String namePrefix;

        private NamedThreadFactory(String namePrefix) {
            if (Strings.isNullOrEmpty(namePrefix)) {
                namePrefix = "SubprocessExecutors";
            }
            checkArgument(CharMatcher.whitespace().negate().countIn(namePrefix) > 0, "name must include non-whitespace");
            this.namePrefix = namePrefix;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Thread newThread(Runnable r) {
            Thread t = defaultFactory.newThread(r);
            String name = String.format("%s-%d", namePrefix, counter.incrementAndGet());
            t.setName(name);
            t.setDaemon(false);
            return t;
        }
    }
}
