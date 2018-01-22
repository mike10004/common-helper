package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface that defines methods for manipulating process output.
 * @param <SO>
 * @param <SE>
 */
public interface ProcessOutputControl<C extends OutputContext, SO, SE> {

    /**
     * Produces the byte sources and byte sinks to be attached to the process
     * standard output, error, and input streams.
     * @return an output context
     */
    C produceContext() throws IOException;

    /**
     * Gets the transform that produces a result from a process exit code.
     * Instances of this class create the process stream sources and sinks
     * (in {@link #produceContext()} and must be able to produce a
     * {@link ProcessOutput} instance after the process has finished.
     * @param exitCode the process exit code
     * @param context the output context produced by {@link #produceContext()}
     * @return the transform
     */
    ProcessOutput<SO, SE> transform(int exitCode, C context);

    /**
     * Maps the types of this output control to other types using a pair of functions.
     * @param stdoutMap the standard output map function
     * @param stderrMap the standard error map function
     * @param <SO2> the destination type for standard output content
     * @param <SE2> the destination type for standard error content
     * @return an output control satisfying the requirements of the destination types
     */
    default <SO2, SE2> ProcessOutputControl<C, SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        ProcessOutputControl<C, SO, SE> self = this;
        return new ProcessOutputControl<C, SO2, SE2>() {
            @Override
            public C produceContext() throws IOException {
                return self.produceContext();
            }

            @Override
            public ProcessOutput<SO2, SE2> transform(int exitCode, C context) {
                return self.transform(exitCode, context).map(stdoutMap, stderrMap);
            }
        };
    }

    /**
     * Process output control whose captured standard output and error content is
     * the same type
     * @param <S> type of the captured standard output and standard error contents
     */
    interface UniformOutputControl<C extends OutputContext, S> extends ProcessOutputControl<C, S, S> {

        /**
         * Wraps a process output control whose standard error and output type
         * in an object that satisfies this interface.
         */
        static <C extends OutputContext, S> UniformOutputControl<C, S> wrap(ProcessOutputControl<C, S, S> homogenous) {
            return new UniformOutputControl<C, S>() {
                @Override
                public C produceContext() throws IOException {
                    return homogenous.produceContext();
                }

                @Override
                public ProcessOutput<S, S> transform(int exitCode, C context) {
                    return homogenous.transform(exitCode, context);
                }
            };
        }

        /**
         * @see ProcessOutputControl#map(Function, Function)
         */
        default <T> UniformOutputControl<C, T> map(Function<? super S, T> stmap) {
            ProcessOutputControl<C, S, S> duplex = this;
            return new UniformOutputControl<C, T>() {

                @Override
                public C produceContext() throws IOException {
                    return duplex.produceContext();
                }

                @Override
                public ProcessOutput<T, T> transform(int exitCode, C context) {
                    return duplex.transform(exitCode, context).map(stmap, stmap);
                }
            };
        }
    }

    static <C extends OutputContext, SO, SE> ProcessOutputControl<C, SO, SE> predefinedAndOutputIgnored(C ctx) {
        return predefined(ctx, ProcessOutputControls::noOutput);
    }

    static <C extends OutputContext, SO, SE> ProcessOutputControl<C, SO, SE> predefined(C outputContext, Supplier<SO> stdoutProvider, Supplier<SE> stderrProvider) {
        return predefined(outputContext, () -> ProcessOutput.direct(stdoutProvider.get(), stderrProvider.get()));
    }

    static <C extends OutputContext, SO, SE> ProcessOutputControl<C, SO, SE> predefined(C outputContext, Supplier<ProcessOutput<SO, SE>> outputter) {
        return new ProcessOutputControl<C, SO, SE>() {
            @Override
            public C produceContext() {
                return outputContext;
            }

            @Override
            public ProcessOutput<SO, SE> transform(int exitCode, C context) {
                return outputter.get();
            }
        };
    }

}
