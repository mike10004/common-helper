/*
 * The MIT License
 *
 * Copyright 2015 mchaberski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mike10004.nativehelper.test;

import com.google.common.io.Files;
import com.novetta.ibg.common.sys.Platform;
import com.novetta.ibg.common.sys.Platforms;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author mchaberski
 */
public abstract class ExternalProcessTestCase {

    public abstract String getName();

    public abstract List<String> buildArguments() throws Exception;

    public abstract int getExpectedExitCode();

    public static class FreebsdNonzeroExitTestCase extends ExternalProcessTestCase {
        
        private final File emptyDir;

        public FreebsdNonzeroExitTestCase(File emptyDir) {
            this.emptyDir = emptyDir;
        }

        @Override
        public String getName() {
            return "ls";
        }

        @Override
        public List<String> buildArguments() throws IOException {
            File file = new File(emptyDir, "fileThatDoesNotExist");
            return Arrays.asList(file.getAbsolutePath());
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
        public String getName() {
            return "ls";
        }

        @Override
        public List<String> buildArguments() throws IOException {
            File file = new File(emptyDir, "fileThatDoesNotExist");
            return Arrays.asList(file.getAbsolutePath());
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
        public String getName() {
            return "ls";
        }

        @Override
        public List<String> buildArguments() throws IOException {
            File file = new File(emptyDir, "newfile");
            Files.touch(file);
            return Arrays.asList(file.getAbsolutePath());
        }

        @Override
        public int getExpectedExitCode() {
            return 0;
        }
        
    }
    
    public static class WindowsZeroExitTestCase extends ExternalProcessTestCase {

        @Override
        public String getName() {
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
        public String getName() {
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
    
    public static ExternalProcessTestCase createTestCase(File emptyDir) {
        Platform platform = Platforms.getPlatform();
        if (platform.isWindows()) {
            return new WindowsNonzeroExitTestCase();
        } else if (platform.isLinux()) {
            return new LinuxNonzeroExitTestCase(emptyDir);
        } else if (platform.isBSD() || platform.isOSX()) {
            return new FreebsdNonzeroExitTestCase(emptyDir);
        } else {
            System.out.println("don't know how to test for platform " + platform);
            return null;
        }
    }
    

}
