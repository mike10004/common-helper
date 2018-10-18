package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.Platforms.UnsupportedPlatformException;
import java.io.File;

/**
 * Interface for objects that represent a given deployment platform. Instances
 * of this class can provide information about properties of the system
 * that are platform-specific.
 */
public interface Platform {

    /**
     * Gets the system configuration file directory. This is {@code /etc} on
     * Linux and the same as the program data directory on Windows.
     * @return pathname of the system configuration file directory
     * @throws UnsupportedPlatformException
     * if this platfgorm is not supported
     * @see #getSystemConfigDir(java.lang.String, java.lang.String...) 
     */
      File getSystemConfigDir() throws UnsupportedPlatformException;

    /**
     * Gets the pathname of the system common program files directory.
     * This is {@code /usr/share} on Linux and the value of the 
     * {@code CommonProgramFiles} environment variable on Windows.
     * @return pathname of the common program files directory
     * @throws UnsupportedPlatformException
     * if this platfgorm is not supported 
     * @see #getCommonProgramFilesDir(java.lang.String, java.lang.String...) 
     */
      File getCommonProgramFilesDir() throws UnsupportedPlatformException;

    /**
     * Gets the pathname of the system program data directory. This is 
     * {@code /var/lib} on Linux and the value of the {@code ProgramData} 
     * environment variable on Windows.
     * @return pathname of the program data directory
     * @throws UnsupportedPlatformException
     * if this platfgorm is not supported 
     * @see #getProgramDataDir(java.lang.String, java.lang.String...) 
     */
      File getProgramDataDir() throws UnsupportedPlatformException;

    /**
     * Gets the pathname of the user configuration file directory.
     * You probably don't want to use this directly, but rather you want 
     * to use a subdirectory. @see #getUserConfigDir(java.lang.String, java.lang.String...)
     * On Linux, this is the user home directory, and on Windows, it's the 
     * value of the {@code AppData} environment variable. 
     * @return pathname of the user configuration file directory
     * @throws UnsupportedPlatformException
     * if this platfgorm is not supported 
     * @see #getUserConfigDir(java.lang.String, java.lang.String...)
     */
      File getUserConfigDir() throws UnsupportedPlatformException;

    /**
     * Gets the pathname of a subdirectory of the system configuration 
     * directory.
     * @param first a path-narrowing component
     * @param rest further path-narrowing components
     * @return pathname of the system configuration file directory
     * @see #getSystemConfigDir() 
     * @throws UnsupportedPlatformException
     * if this platfgorm is not supported
     */
     File getSystemConfigDir(String first, String... rest)  throws UnsupportedPlatformException;

    /**
     * Gets the pathname of a subdirectory of the system program data 
     * directory.
     * @param first a path-narrowing component
     * @param rest further path-narrowing components
     * @return the pathname
     * @see #getProgramDataDir() 
     * @throws UnsupportedPlatformException
     * if this platfgorm is not supported
     */
     File getProgramDataDir(String first, String... rest)  throws UnsupportedPlatformException;

    /**
     * Gets the pathname of a subdirectory of the user configuration files
     * directory. On Windows, this is the subdirectory specified by the
     * lineage given by the arguments. On Linux, the first argument is 
     * prefixed with a dot, and the rest are taken as they are.
     * @param first a path-narrowing component
     * @param rest further path-narrowing components
     * @return pathname of the user configuration file directory
     * @throws UnsupportedPlatformException
     * if this platfgorm is not supported
     */
     File getUserConfigDir(String first, String...rest)  throws UnsupportedPlatformException;

     File getCommonProgramFilesDir(String first, String... rest)  throws UnsupportedPlatformException;

     /**
      * Checks whether this platform is Windows.
      * @return true if Windows
      */
    boolean isWindows();
    
    /**
     * Checks whether this platform is Linux.
     * @return true if Linux
     */
    boolean isLinux();
    
    /**
     * Checks whether this platform is UNIX-like (but not Linux or OS X or BSD).
     * @return true if UNIX-like
     */
    boolean isUnix();
    
    /**
     * Checks whether this platform is OS X.
     * @return true if OS X
     */
    boolean isOSX();

    /**
     * Checks whether this platform is BSD.
     * @return true if BSD
     */
    boolean isBSD();
}

