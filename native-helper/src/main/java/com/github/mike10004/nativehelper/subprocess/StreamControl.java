package com.github.mike10004.nativehelper.subprocess;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface StreamControl {

    OutputStream openStdoutSink() throws IOException;
    OutputStream openStderrSink() throws IOException;
    @Nullable
    InputStream openStdinSource() throws IOException;

}
