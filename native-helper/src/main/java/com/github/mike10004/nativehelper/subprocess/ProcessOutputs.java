package com.github.mike10004.nativehelper.subprocess;

import com.google.common.base.Suppliers;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;
import java.util.function.Supplier;

class ProcessOutputs {

    public static class DirectOutput<SO, SE> implements ProcessOutput<SO, SE> {

        private final SO stdout;
        private final SE stderr;

        public DirectOutput(SO stdout, SE stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        @Override
        public SO getStdout() {
            return stdout;
        }

        @Override
        public SE getStderr() {
            return stderr;
        }

        @Override
        public String toString() {
            return "DirectOutput{" +
                    "stdout=" + abbrev(stdout )+
                    ", stderr=" + abbrev(stderr) +
                    '}';
        }
    }

    private static String abbrev(Object object) {
        String abbreved = StringUtils.abbreviateMiddle(object.toString(), "...", ABBREV);
        return StringEscapeUtils.escapeJava(abbreved);
    }

    public static class DeferredOutput<SO, SE> implements ProcessOutput<SO, SE> {

        private final Supplier<SO> stdoutSupplier;
        private final Supplier<SE> stderrSupplier;

        public DeferredOutput(Supplier<SO> stdoutSupplier, Supplier<SE> stderrSupplier) {
            this.stdoutSupplier = Suppliers.memoize(stdoutSupplier::get);
            this.stderrSupplier = Suppliers.memoize(stderrSupplier::get);
        }

        @Override
        public SO getStdout() {
            return stdoutSupplier.get();
        }

        @Override
        public SE getStderr() {
            return stderrSupplier.get();
        }

        @Override
        public String toString() {
            return "DeferredOutput{" +
                    "stdoutSupplier=" + stdoutSupplier +
                    ", stderrSupplier=" + stderrSupplier +
                    '}';
        }

        public String toStringExpanded() {
            return "DeferredOutput:Expanded{" +
                    "stdout=" + abbrev(getStdout().toString()) +
                    ", stderr=" + abbrev(getStderr().toString()) +
                    '}';
        }

    }

    private static final int ABBREV = 64;

    public static class MappedOutput<SO, SE> extends DeferredOutput<SO, SE> {

        public <SO0, SE0> MappedOutput(ProcessOutput<SO0, SE0> original, Function<? super SO0, SO> stdoutMap, Function<? super SE0, SE> stderrMap) {
            super(() -> stdoutMap.apply(original.getStdout()), () -> stderrMap.apply(original.getStderr()));
        }

        @Override
        public String toString() {
            return toStringExpanded();
        }
    }

    @SuppressWarnings("unchecked")
    public static <SO, SE> ProcessOutput<SO, SE> bothNull() {
        return BOTH_NULL;
    }

    private static final ProcessOutput BOTH_NULL = ProcessOutput.direct(null, null);
}