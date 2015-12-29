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
package com.github.mike10004.nativehelper;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author mchaberski
 */
public class BuilderTests {

    private BuilderTests() {}
    
    public static void confirmBuilderSubclassExists(Class programSubclass) throws ClassNotFoundException { 
        Class<?> builderSubclass = Class.forName(programSubclass.getName() + "$Builder");
        assertFalse(Program.Builder.class.equals(builderSubclass));
    }
    
    public static void confirmAllSuperclassBuilderSetterMethodsAreOverridden(Class<? extends Program.Builder> builderSubclass) {
        System.out.println("confirmAllSuperclassBuilderSetterMethodsAreOverridden in " + builderSubclass);
        checkArgument(!builderSubclass.equals(Program.Builder.class));
        final Class<?> superclass = Program.Builder.class;
        Predicate<Method> isBuilderSetter = new Predicate<Method>(){
            @Override
            public boolean apply(Method m) {
                return Modifier.isPublic(m.getModifiers())
                        && !Modifier.isStatic(m.getModifiers())
                        && superclass.equals(m.getReturnType());
            }
        };
        Iterable<Method> superclassMethods = Arrays.asList(superclass.getDeclaredMethods());
        superclassMethods = Iterables.filter(superclassMethods, isBuilderSetter);
        
        Iterable<Method> subclassMethods = Arrays.asList(builderSubclass.getDeclaredMethods());
        Iterable<Method> nonBridgeSubclassMethods = Iterables.filter(subclassMethods, new Predicate<Method>(){
            @Override
            public boolean apply(Method method) {
                return Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())
                        && !method.isBridge();
            }
        });
        for (Method superclassMethod : superclassMethods) {
            boolean found = false;
            for (Method subclassMethod : nonBridgeSubclassMethods) {
                if (isOverriddenMethod(superclassMethod, subclassMethod)) {
                    System.out.format("%s.%s overrides %s.%s%n", packageless(builderSubclass), subclassMethod.getName(), packageless(superclass), superclassMethod.getName());
                    found = true;
                    break;
                }
            }
            assertTrue("subclass must override method " + superclassMethod, found);
        }
    }
    
    private static boolean isOverriddenMethod(Method superclassMethod, Method subclassMethod) {
        return superclassMethod.getName().equals(subclassMethod.getName()) 
                && Arrays.deepEquals(superclassMethod.getParameterTypes(), subclassMethod.getParameterTypes());
    }
    
    public static void confirmAllBuilderSetterMethodsReturnSubclass(final Class<? extends Program.Builder> builderSubclass) {
        System.out.println("confirmAllBuilderSetterMethodsReturnSubclass in " + builderSubclass);
        checkArgument(!builderSubclass.equals(Program.Builder.class));
        Iterable<Method> setterMethods = Arrays.asList(builderSubclass.getDeclaredMethods());
        Function<Class, String> classnamer = new Function<Class, String>(){
            @Override
            public String apply(Class input) {
                return packageless(input);
            }
        };
        System.out.println(builderSubclass + " methods:");
        setterMethods = Iterables.filter(setterMethods, new Predicate<Method>(){
            @Override
            public boolean apply(Method method) {
                return Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())
                        && !method.isBridge()
                        && Program.Builder.class.isAssignableFrom(method.getReturnType());
            }
        });
        for (Method method : setterMethods) {
            Iterable<String> parameters = Iterables.transform(Arrays.asList(method.getParameterTypes()), classnamer);
            System.out.format("  %s %s(%s)%n", packageless(method.getReturnType()), method.getName(), Joiner.on(", ").join(parameters));
        }
        boolean atLeastOneChecked = false;
        for (Method method : setterMethods) {
            Class<?> returnType = method.getReturnType();
            System.out.format("%s returns %s%n", method, returnType);
            assertEquals("expect return " + packageless(builderSubclass) + " but actual is " + packageless(returnType), builderSubclass, returnType);
            atLeastOneChecked = true;
        }
        assertTrue(atLeastOneChecked);
    }
    
    private static String packageless(Class<?> clz) {
        if (clz == null) return "null";
        if (clz.isArray()) {
            return packageless(clz.getComponentType()) + "[]";
        }
        if (clz.getPackage() != null) {
            return StringUtils.removeStart(clz.getName(), clz.getPackage().getName() + '.');
        } else {
            return clz.getName();
        }
    }
}
