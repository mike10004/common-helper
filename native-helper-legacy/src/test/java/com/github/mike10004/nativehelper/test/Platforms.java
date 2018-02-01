package com.github.mike10004.nativehelper.test;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.getenv;

/**
 * Class that provides static methods relating to application 
 * deployment platforms.
 * 
 * @author mchaberski
 * @see Platform
 */
public final class Platforms {
    
    public final static String OS = System.getProperty("os.name");
    
    private Platforms() {}

    public static Platform getPlatform(String osName) {
        Class<? extends Platform> platformClass = getPlatformClass(osName);
        return cache.getUnchecked(platformClass);
    }
    
    public static Platform getPlatform() {
        Class<? extends Platform> platformClass = detectPlatformClass();
        /*
         * We can get this value with an unchecked call because detectPlatformClass()
         * only returns classes we know are instantiable because they are all
         * defined here. If future improvements to this class allow for 
         * plugging in external Platform implementations, then it might be 
         * wise to make this a checked call and add a throws declaration to
         * the getPlatform() function.
         */
        return cache.getUnchecked(platformClass);
    }
    
    private static final LoadingCache<Class<? extends Platform>, Platform> cache 
            = CacheBuilder.newBuilder()
            .build(new CacheLoader<Class<? extends Platform>, Platform>() {
        @Override
        public Platform load(Class<? extends Platform> key) throws InstantiationException, IllegalAccessException {
            return key.newInstance();
        }
    });
    
    private static Class<? extends Platform> detectPlatformClass() {
        return getPlatformClass(OS);
    }
    
    public static Class<? extends Platform> getPlatformClass(String osName) {
        if (osName.endsWith("BSD")) {
            return Bsd.class;
        } else if (osName.startsWith("Linux")) {
            return Linux.class;
        } else if (osName.startsWith("Windows 2003") || osName.startsWith("Windows Server 2003")) {
            return WindowsServer2003.class;
        } else if (osName.startsWith("Windows Server 2008") || osName.startsWith("Windows 2008")) {
            return WindowsServer2008.class;
        } else if (osName.startsWith("Windows Server 2008") || osName.startsWith("Windows 2008")) {
            return WindowsServer2012.class;
        } else if (osName.startsWith("Windows 7")) {
            return Windows7.class;
        } else if (osName.startsWith("Windows 8")) {
            return Windows8.class;
        } else if (osName.startsWith("Windows XP")) {
            return WindowsXP.class;
        }  else if (osName.startsWith("Windows Vista")) {
            return WindowsVista.class;
        } else if (osName.startsWith("Windows")) {
            return WindowsLike.class;
        } else if (osName.equals("Mac OS X")) {
            return OSXLike.class;
        }
        Logger.getLogger(Platforms.class.getName())
                .log(Level.WARNING,"{0}" + " represents an unknown platform; functionality "
                        + "involving system and user directory detection may "
                        + "not work", osName);
        return NullPlatform.class;
    }

    enum OsType {
        WINDOWS,
        LINUX,
        UNIX,
        OSX,
        BSD
    }
    
    static abstract class AbstractPlatform implements Platform {
        
        private final OsType osType;

        public AbstractPlatform(OsType osType) {
            this.osType = osType;
        }

        /**
         * Gets the pathname of a subdirectory of the system configuration 
         * directory.
         * @param first
         * @param rest
         * @return
         * @see #getSystemConfigDir() 
         * @throws UnsupportedPlatformException
         */
        @Override
        public File getSystemConfigDir(String first, String... rest)  throws UnsupportedPlatformException{
            Iterable<String> all = Lists.asList(first, rest);
            return join(getSystemConfigDir(), all);
        }

        /**
         * Gets the pathname of a subdirectory of the system program data 
         * directory.
         * @param first
         * @param rest
         * @return the pathname
         * @see #getProgramDataDir() 
         * @throws UnsupportedPlatformException
         */
        @Override
        public File getProgramDataDir(String first, String... rest)  throws UnsupportedPlatformException{
            Iterable<String> all = Lists.asList(first, rest);
            return join(getProgramDataDir(), all);
        }

        /**
         * Gets the pathname of a subdirectory of the user configuration files
         * directory. On Windows, this is the subdirectory specified by the
         * lineage given by the arguments. On Linux, the first argument is 
         * prefixed with a dot, and the rest are taken as they are.
         * @param first
         * @param rest
         * @return
         * @throws UnsupportedPlatformException
         */
        @Override
        public File getUserConfigDir(String first, String...rest)  throws UnsupportedPlatformException{
            Iterable<String> all = Lists.asList(first, rest);
            return join(getUserConfigDir(), all);
        }

        protected File join(File ancestor, Iterable<String> lineage) {
            return joinPathComponents(ancestor, lineage);
        }
        
        private static File joinPathComponents(File root, Iterable<String> components) {
            for (String c : components) {
                root = new File(root, c);
            }
            return root;
        }

        @Override
        public File getCommonProgramFilesDir(String first, String... rest)  throws UnsupportedPlatformException{
            List<String> all = Lists.asList(first, rest);//, rest)
            return join(getCommonProgramFilesDir(), all);
        }
        @Override
        public boolean isLinux() {
            return osType == OsType.LINUX;
        }

        @Override
        public boolean isOSX() {
            return osType == OsType.OSX;
        }

        @Override
        public boolean isUnix() {
            return osType == OsType.UNIX;
        }

        @Override
        public boolean isWindows() {
            return osType == OsType.WINDOWS;
        }

        @Override
        public boolean isBSD() {
            return osType == OsType.BSD;
        }
        
    }
    
    public static class UnsupportedPlatformException extends UnsupportedOperationException {

        public UnsupportedPlatformException(String functionality) {
            super(functionality + " is not supported on this platform");
        }
        
    }
    
    static class NullPlatform extends AbstractPlatform {

        public NullPlatform() {
            super(null);
        }
        
        @Override
        public File getCommonProgramFilesDir() {
            throw new UnsupportedPlatformException("getCommonProgramFilesDir");
        }

        @Override
        public File getSystemConfigDir() {
            throw new UnsupportedPlatformException("getSystemConfigDir");
        }

        @Override
        public File getProgramDataDir() {
            throw new UnsupportedPlatformException("getProgramDataDir");
        }

        @Override
        public File getUserConfigDir() {
            throw new UnsupportedPlatformException("getUserConfigDir");
        }

        @Override
        public boolean isWindows() {
            return false;
        }

        @Override
        public boolean isLinux() {
            return false;
        }

        @Override
        public boolean isUnix() {
            return false;
        }

        @Override
        public boolean isOSX() {
            return false;
        }
        
    }
    
    static class Linux extends UnixLike {
        
        public Linux() {
            super(OsType.LINUX);
        }
    }
    
    static class Bsd extends UnixLike {
        
        public Bsd() {
            super(OsType.BSD);
        }

        @Override
        public File getCommonProgramFilesDir() {
            return new File("/usr/local/share");
        }

        @Override
        public File getSystemConfigDir() {
            return new File("/usr/local/etc");
        }

    }
    
    static class UnixLike extends AbstractPlatform {

        public UnixLike(OsType osType) {
            super(osType);
        }
        
        @Override
        public File getCommonProgramFilesDir() {
            return new File("/usr/share");
        }

        @Override
        public File getSystemConfigDir() {
            return new File("/etc");
        }

        @Override
        public File getProgramDataDir() {
            return new File("/var/lib");
        }

        @Override
        public File getUserConfigDir() {
            return new File(System.getProperty("user.home"));
        }

        /**
         * Gets the pathname of a subdirectory of the user configuration
         * directory specified by a program name. A dot is prepended 
         * to the program name, because user configuration subdirectories
         * on Linux are usually hidden.
         * @param progname
         * @return 
         */
        @Override
        public File getUserConfigDir(String first, String...rest) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(first));
            first = '.' + first;
            return super.getUserConfigDir(first, rest);
        }

    }
    
    private static class WindowsLike extends AbstractPlatform {

        public WindowsLike() {
            super(OsType.WINDOWS);
        }
        
        @Override
        public File getCommonProgramFilesDir() {
            return new File(getenv("CommonProgramFiles"));
        }

        @Override
        public File getProgramDataDir() {
            return new File(getenv("ProgramData"));
        }

        @Override
        public File getSystemConfigDir() {
            return getProgramDataDir();
        }

        @Override
        public File getUserConfigDir() {
            return new File(getenv("APPDATA"));
        }
        
    }
    
    static class WindowsServer2003 extends WindowsXPLike  {

        
    }
    
    static class WindowsServer2008 extends Windows7Like {
        
    }
    
    static class WindowsServer2012 extends WindowsServer2008 {
        
    }
    
    static class Windows7 extends Windows7Like {
        
    }
    
    static class Windows7Like extends WindowsLike {

    }
    
    static class Windows8Like extends WindowsLike {
        
    }
    
    static class Windows8 extends Windows8Like {
        
    }
    
//    public static File getUserHome() {
//        return new File(System.getProperty("user.home"));
//    }
//    
    static class WindowsXPLike extends WindowsLike {

        @Override
        public File getProgramDataDir() {
            return new File(getenv("ALLUSERSPROFILE"), "Application Data");
        }
    }

    static class WindowsVista extends Windows7Like {
        
    }
    
    static class WindowsXP extends WindowsXPLike {
        
    }
    
    static class OSXLike extends UnixLike {
        
        public OSXLike() {
            super(OsType.OSX);
        }

        @Override
        public File getUserConfigDir() {
            File parent = new File(System.getProperty("user.home"));
            return new File(parent, "Library/Preferences");
        }

        @Override
        public File getSystemConfigDir() {
            return new File("Library/Preferences");
        }
        
    }
}