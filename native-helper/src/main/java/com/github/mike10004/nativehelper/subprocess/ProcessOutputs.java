package com.github.mike10004.nativehelper.subprocess;

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
    }

    @SuppressWarnings("unchecked")
    public static <SO, SE> ProcessOutput<SO, SE> bothNull() {
        return BOTH_NULL;
    }

    private static final ProcessOutput BOTH_NULL = new DirectOutput<>(null, null);
}
