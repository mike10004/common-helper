package com.github.mike10004.nativehelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

/**
 * Interface that defines a method to determine the pathname of
 * an executable. A given implementation could be a generic file resolver,
 * but the {@link #gnu()} implementation approximates the GNU {@code which}
 * program, which only finds existing files that are executable.
 * @author mchaberski
 * @since 3.1.0
 */
public interface Whicher {
    
    /**
     * Returns the which'd value for a single filename.
     * @param filename the filename to search for
     * @return the {@code File} object, or null if not found
     * @see #which(java.lang.Iterable) 
     */
    default Optional<File> which(String filename) {
        return which(ImmutableList.of(filename));
    }

    /**
     * Returns the first which'd value found for a sequence of filenames.
     * @param filenames the filenames to search for 
     * @return the {@code File} object, or null if not found
     */
    Optional<File> which(Iterable<String> filenames);

    /**
     * Constructs and returns a GNU-style instance that searches the system
     * path for executable files, checking Windows extensions if the platform
     * is Windows.
     * @return a constructed instance
     */
    static Whicher gnu() {
        return StandardWhicher.gnu();
    }
    
    /**
     * Creates a new builder with no parent directories to be searched.
     * Warning: you'll never find anything if you don't add any parents.
     * @return the builder
     */
    static StandardWhicher.Builder builder() {
        return new StandardWhicher.Builder();
    }
    
    /**
     * Creates a new builder.
     * @param parent the first parent directory to be searched
     * @return the builder
     */
    static StandardWhicher.Builder builder(File parent) {
        return new StandardWhicher.Builder().in(parent);
    }

    @VisibleForTesting
    static Whicher predefined(@Nullable File returnValue) {
        return new Whicher() {
            @Override
            public Optional<File> which(Iterable<String> filenames) {
                return Optional.ofNullable(returnValue);
            }
        };
    }

    @VisibleForTesting
    static Whicher empty() {
        return new Whicher() {
            @Override
            public Optional<File> which(Iterable<String> filenames) {
                return Optional.empty();
            }
        };
    }
}