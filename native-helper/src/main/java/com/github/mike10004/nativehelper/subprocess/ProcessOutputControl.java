package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.util.function.Function;

public interface ProcessOutputControl<SO, SE> {

    ProcessStreamEndpoints produceEndpoints() throws IOException;

    Function<Integer, ProcessResult<SO, SE>> getTransform();

    default <SO2, SE2> ProcessOutputControl<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        ProcessOutputControl<SO, SE> self = this;
        return new ProcessOutputControl<SO2, SE2>() {
            @Override
            public ProcessStreamEndpoints produceEndpoints() throws IOException {
                return self.produceEndpoints();
            }

            @Override
            public Function<Integer, ProcessResult<SO2, SE2>> getTransform() {
                return self.getTransform().andThen(result -> {
                    return result.map(stdoutMap, stderrMap);
                });
            }
        };
    }

    interface UniformOutputControl<S> extends ProcessOutputControl<S, S> {

        static <S> UniformOutputControl<S> wrap(ProcessOutputControl<S, S> homogenous) {
            return new UniformOutputControl<S>() {
                @Override
                public ProcessStreamEndpoints produceEndpoints() throws IOException {
                    return homogenous.produceEndpoints();
                }

                @Override
                public Function<Integer, ProcessResult<S, S>> getTransform() {
                    return homogenous.getTransform();
                }
            };
        }

        default <T> UniformOutputControl<T> map(Function<? super S, T> stmap) {
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
                            return result.map(stmap, stmap);
                        }
                    };
                }
            };
        }
    }
}
