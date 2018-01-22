package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.util.function.Function;

/**
 * Interface that defines methods for manipulating process output.
 * @param <SO>
 * @param <SE>
 */
public interface ProcessOutputControl<SO, SE> {

    /**
     * Produces the byte sources and byte sinks to be attached to the process
     * standard output, error, and input streams.
     * @return the endpoints instance
     */
    ProcessStreamEndpoints produceEndpoints() throws IOException;

    /**
     * Gets the transform that produces a result from a process exit code.
     * Instances of this class create the process stream sources and sinks
     * (in {@link #produceEndpoints()} and must be able to produce a
     * {@link ProcessOutput} instance after the process has finished.
     * @return the transform
     */
    Function<Integer, ProcessResult<SO, SE>> getTransform();

    /**
     * Maps the types of this output control to other types using a pair of functions.
     * @param stdoutMap the standard output map function
     * @param stderrMap the standard error map function
     * @param <SO2> the destination type for standard output content
     * @param <SE2> the destination type for standard error content
     * @return an output control satisfying the requirements of the destination types
     */
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

    /**
     * Process output control whose captured standard output and error content is
     * the same type
     * @param <S> type of the captured standard output and standard error contents
     */
    interface UniformOutputControl<S> extends ProcessOutputControl<S, S> {

        /**
         * Wraps a process output control whose standard error and output type
         * in an object that satisfies this interface.
         */
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

        /**
         * @see ProcessOutputControl#map(Function, Function)
         */
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
