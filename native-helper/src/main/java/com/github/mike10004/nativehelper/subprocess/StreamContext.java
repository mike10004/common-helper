package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface that defines methods for manipulating process output.
 * @param <SO>
 * @param <SE>
 */
public interface StreamContext<C extends StreamControl, SO, SE> {

    /**
     * Produces the byte sources and byte sinks to be attached to the process
     * standard output, error, and input streams.
     * @return an output context
     */
    C produceControl() throws IOException;

    /**
     * Gets the transform that produces a result from a process exit code.
     * Instances of this class create the process stream sources and sinks
     * (in {@link #produceControl()} and must be able to produce a
     * {@link StreamContent} instance after the process has finished.
     * @param exitCode the process exit code
     * @param context the output context produced by {@link #produceControl()}
     * @return the transform
     */
    StreamContent<SO, SE> transform(int exitCode, C context);

    /**
     * Maps the types of this output control to other types using a pair of functions.
     * @param stdoutMap the standard output map function
     * @param stderrMap the standard error map function
     * @param <SO2> the destination type for standard output content
     * @param <SE2> the destination type for standard error content
     * @return an output control satisfying the requirements of the destination types
     */
    default <SO2, SE2> StreamContext<C, SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        StreamContext<C, SO, SE> self = this;
        return new StreamContext<C, SO2, SE2>() {
            @Override
            public C produceControl() throws IOException {
                return self.produceControl();
            }

            @Override
            public StreamContent<SO2, SE2> transform(int exitCode, C context) {
                return self.transform(exitCode, context).map(stdoutMap, stderrMap);
            }
        };
    }

    /**
     * Process output control whose captured standard output and error content is
     * the same type
     * @param <S> type of the captured standard output and standard error contents
     */
    interface UniformStreamContext<C extends StreamControl, S> extends StreamContext<C, S, S> {

        /**
         * Wraps a process output control whose standard error and output type
         * in an object that satisfies this interface.
         */
        static <C extends StreamControl, S> UniformStreamContext<C, S> wrap(StreamContext<C, S, S> homogenous) {
            return new UniformStreamContext<C, S>() {
                @Override
                public C produceControl() throws IOException {
                    return homogenous.produceControl();
                }

                @Override
                public StreamContent<S, S> transform(int exitCode, C context) {
                    return homogenous.transform(exitCode, context);
                }
            };
        }

        /**
         * @see StreamContext#map(Function, Function)
         */
        default <T> UniformStreamContext<C, T> map(Function<? super S, T> stmap) {
            StreamContext<C, S, S> duplex = this;
            return new UniformStreamContext<C, T>() {

                @Override
                public C produceControl() throws IOException {
                    return duplex.produceControl();
                }

                @Override
                public StreamContent<T, T> transform(int exitCode, C context) {
                    return duplex.transform(exitCode, context).map(stmap, stmap);
                }
            };
        }
    }

    static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefinedAndOutputIgnored(C ctx) {
        return predefined(ctx, StreamContexts::noOutput);
    }

    static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefined(C outputContext, Supplier<SO> stdoutProvider, Supplier<SE> stderrProvider) {
        return predefined(outputContext, () -> StreamContent.direct(stdoutProvider.get(), stderrProvider.get()));
    }

    static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefined(C outputContext, Supplier<StreamContent<SO, SE>> outputter) {
        return new StreamContext<C, SO, SE>() {
            @Override
            public C produceControl() {
                return outputContext;
            }

            @Override
            public StreamContent<SO, SE> transform(int exitCode, C context) {
                return outputter.get();
            }
        };
    }

}
