package com.github.mike10004.nativehelper;

import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MoreProgramTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void zeroExit() throws Exception {
        perform(ExternalProcessTestCase.createZeroExitTestCase(temporaryFolder.newFolder()));
    }

    @Test
    public void nonzeroExit() throws Exception {
        perform(ExternalProcessTestCase.createNonzeroExitTestCase(temporaryFolder.newFolder()));
    }

    private void perform(ExternalProcessTestCase testCase) throws Exception {
        Program.Builder pb = Program.running(testCase.getExecutableName());
        pb.args(testCase.buildArguments());
        Program<?> program = pb.ignoreOutput();
        ProgramResult result = program.execute();
        int expectedExitCode = testCase.getExpectedExitCode();
        assertEquals("exit code", expectedExitCode, result.getExitCode());
    }

    private static abstract class ExternalProcessTestCase {

        public abstract String getExecutableName();

        public abstract List<String> buildArguments() throws Exception;

        public abstract int getExpectedExitCode();

        public static class FreebsdNonzeroExitTestCase extends ExternalProcessTestCase {

            private final File emptyDir;

            public FreebsdNonzeroExitTestCase(File emptyDir) {
                this.emptyDir = emptyDir;
            }

            @Override
            public String getExecutableName() {
                return "ls";
            }

            @Override
            public List<String> buildArguments() throws IOException {
                File file = new File(emptyDir, "fileThatDoesNotExist");
                return Collections.singletonList(file.getAbsolutePath());
            }

            @Override
            public int getExpectedExitCode() {
                return 1;
            }

        }

        public static class LinuxNonzeroExitTestCase extends ExternalProcessTestCase {

            private final File emptyDir;

            public LinuxNonzeroExitTestCase(File emptyDir) {
                this.emptyDir = emptyDir;
            }

            @Override
            public String getExecutableName() {
                return "ls";
            }

            @Override
            public List<String> buildArguments() throws IOException {
                File file = new File(emptyDir, "fileThatDoesNotExist");
                return Collections.singletonList(file.getAbsolutePath());
            }

            @Override
            public int getExpectedExitCode() {
                return 2;
            }

        }

        public static class NonwindowsZeroExitTestCase extends ExternalProcessTestCase {

            private final File emptyDir;

            public NonwindowsZeroExitTestCase(File emptyDir) {
                this.emptyDir = emptyDir;
            }

            @Override
            public String getExecutableName() {
                return "ls";
            }

            @Override
            public List<String> buildArguments() throws IOException {
                File file = new File(emptyDir, "newfile");
                Files.touch(file);
                return Collections.singletonList(file.getAbsolutePath());
            }

            @Override
            public int getExpectedExitCode() {
                return 0;
            }

        }

        public static class WindowsZeroExitTestCase extends ExternalProcessTestCase {

            @Override
            public String getExecutableName() {
                return "cmd";
            }

            @Override
            public List<String> buildArguments() {
                return Arrays.asList("/C", "echo", "hello, world");
            }

            @Override
            public int getExpectedExitCode() {
                return 0;
            }

        }

        public static class WindowsNonzeroExitTestCase extends ExternalProcessTestCase {

            @Override
            public String getExecutableName() {
                return "cmd";
            }

            @Override
            public List<String> buildArguments() {
                return Arrays.asList("/C", "exit 1");
            }

            @Override
            public int getExpectedExitCode() {
                return 1;
            }

        }

        public static ExternalProcessTestCase createNonzeroExitTestCase(File emptyDir) {
            Platform platform = Platforms.getPlatform();
            if (platform.isWindows()) {
                return new WindowsNonzeroExitTestCase();
            } else if (platform.isLinux()) {
                return new LinuxNonzeroExitTestCase(emptyDir);
            } else if (platform.isBSD() || platform.isOSX()) {
                return new FreebsdNonzeroExitTestCase(emptyDir);
            } else {
                throw new IllegalStateException("platform not handled: " + platform); // maybe Assume.assumeTrue(false) here
            }
        }

        public static ExternalProcessTestCase createZeroExitTestCase(File emptyDir) {
            Platform platform = Platforms.getPlatform();
            if (platform.isWindows()) {
                return new WindowsZeroExitTestCase();
            } else {
                return new NonwindowsZeroExitTestCase(emptyDir);
            }
        }


    }

}
