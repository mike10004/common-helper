package com.github.mike10004.nativehelper.subprocess;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;

class ExecutorServices {

    private ExecutorServices() {}

    private static final CharMatcher VALID_POOL_NAME_CHARS = CharMatcher.inRange('a', 'z')
            .or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.inRange('0', '9'))
            .or(CharMatcher.anyOf("_-.,"));

    public static Supplier<ListeningExecutorService> newSingleThreadExecutorServiceFactory(String poolName) {
        checkArgument(VALID_POOL_NAME_CHARS.matchesAllOf(poolName), "pool name characters are restricted to %s", VALID_POOL_NAME_CHARS);
        return () -> {
            String prefix = Strings.isNullOrEmpty(poolName) ? "nh-subprocess" : poolName;
            ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(prefix + "-%d")
                    .build();
            ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);
            return MoreExecutors.listeningDecorator(service);
        };
    }

}
