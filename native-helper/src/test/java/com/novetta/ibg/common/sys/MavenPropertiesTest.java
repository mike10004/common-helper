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
package com.novetta.ibg.common.sys;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class MavenPropertiesTest {
    
    private static final String PROPERTIES_PATH = "/native-helper/maven.properties";
    
    @Test
    public void testPropertiesAreFiltered() {
        System.out.println("testPropertiesAreFiltered");
        
        Properties p = loadMavenProperties();
        System.out.println(p.size() + " properties defined");
        Set<String> expectedPropertyNames = ImmutableSet.of("project.artifactId", "project.groupId", "project.version", "project.name");
        for (String propertyName : expectedPropertyNames) {
            String value = p.getProperty(propertyName);
            System.out.format("%s = %s%n", propertyName, value);
            assertNotNull("expected property to be defined", value);
            assertNotUnfiltered(p, propertyName);
        }
    }
    
    private void assertNotUnfiltered(Properties p, String propertyName) {
        String propertyValue = p.getProperty(propertyName);
        checkArgument(propertyValue != null, "non-null property value required");
        assertFalse(propertyName + " property value is not filtered: " + StringUtils.abbreviate(propertyValue, 128), propertyValue.startsWith("${"));
    }
    
    private static Properties loadMavenProperties() {
        Properties p = new Properties();
        try (InputStream in = ExposedExecTask.class.getResourceAsStream(PROPERTIES_PATH)) {
            p.load(in);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return p;
    }
    
}
