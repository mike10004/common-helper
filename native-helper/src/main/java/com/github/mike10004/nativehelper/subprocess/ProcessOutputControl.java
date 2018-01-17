package com.github.mike10004.nativehelper.subprocess;

import com.google.common.util.concurrent.AsyncFunction;

import java.io.IOException;
import java.util.function.Function;

public interface ProcessOutputControl<SO, SE> {

    ProcessStreamEndpoints produceEndpoints() throws IOException;

    Function<Integer, ProcessResult<SO, SE>> getTransform();

    default <SO2, SE2> ProcessOutputControl<SO2, SE2> map(Function<SO, SO2> stdoutMap, Function<SE, SE2> stderrMap) {

    }

    interface UniformOutputControl<S> extends ProcessOutputControl<S, S> {

        default <T> UniformOutputControl<T> map(Function<S, T> function) {
            ProcessOutputControl<S, S> duplex = this;
            return new UniformOutputControl<T>() {

                @Override
                public ProcessStreamEndpoints produceEndpoints() throws IOException {
                    return duplex.produceEndpoints();
                }

                @Override
                public Function<Integer, ProcessResult<T, T>> getTransform() {
                    Function<Integer, ProcessResult<S, S>> f = duplex.getTransform();
                    return new Function<Integer, ProcessResult<T, T>>() {
                        @Override
                        public ProcessResult<T, T> apply(Integer integer) {
                            ProcessResult<S, S> result = f.apply(integer);
                        }
                    }
                }
            }
        }
    }
}
