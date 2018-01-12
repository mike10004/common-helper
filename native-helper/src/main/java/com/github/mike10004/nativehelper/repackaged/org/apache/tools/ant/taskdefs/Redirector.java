/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.github.mike10004.nativehelper.repackaged.org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;

/**
 * The Redirector class manages the setup and connection of input and output
 * redirection for an Ant project component.
 *
 * @since Ant 1.6
 */
@SuppressWarnings("unused")
public interface Redirector {
    void setLogError(boolean logError);

    void setErrorProperty(String errorProperty);

    void setAppend(boolean append);

    void setInput(File input);

    void setInputString(String input);

    void setOutput(File output);

    void setError(File error);

    void setOutputProperty(String propertyName);

    void complete() throws IOException;

    ExecuteStreamHandler createHandler();
}
