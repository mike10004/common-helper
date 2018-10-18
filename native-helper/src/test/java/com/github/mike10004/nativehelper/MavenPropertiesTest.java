package com.github.mike10004.nativehelper;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import static org.junit.Assert.*;

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
        try (InputStream in = MavenPropertiesTest.class.getResourceAsStream(PROPERTIES_PATH)) {
            p.load(in);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return p;
    }
    
}
