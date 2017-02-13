package com.github.mike10004.nativehelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class that provides a method of determining the pathname of 
 * an executable. This class's functionality approximates that of the GNU 
 * {@code which} program.
 * @author mchaberski
 * @since 3.1.0
 */
public class Whicher {
    
    private final ImmutableSet<File> parents;
    private final Predicate<File> validPredicate;
    private final Function<String, Iterable<String>> transform;
    
    public Whicher(Iterable<File> parents, Predicate<File> validPredicate) {
        this(parents, validPredicate, IdentityTransform.instance);
    }
    
    public Whicher(Iterable<File> parents, Predicate<File> validPredicate, Function<String, Iterable<String>> transform) {
        super();
        this.validPredicate = checkNotNull(validPredicate);
        this.parents = ImmutableSet.copyOf(checkNotNull(parents));
        this.transform = checkNotNull(transform);
    }

    @VisibleForTesting
    static Predicate<File> newIsExecutablePredicate() {
        return new Predicate<File>() {
            @Override
            public boolean apply(File input) {
                return input.canExecute();
            }
        };
    }
    
    @VisibleForTesting
    static Predicate<File> newIsFilePredicate() {
        return new Predicate<File>() {
            @Override
            public boolean apply(File input) {
                return input.isFile();
            }
        };
    }
    
    /**
     * Returns the which'd value for a single filename.
     * @param filename the filename to search for
     * @return the {@code File} object, or null if not found
     * @see #which(java.lang.Iterable) 
     */
    public Optional<File> which(String filename) {
        return which(ImmutableList.of(filename));
    }
    
    /**
     * Returns the {@code File} object representing the pathname that is 
     * the result of joining a parent pathname with the argument filename and
     * is valid for a given predicate. The predicate is checked with 
     * {@link #isValidResult(java.io.File) }. 
     * @param filenames the filenames to search for 
     * @return the {@code File} object, or null if not found
     */
    public Optional<File> which(Iterable<String> filenames) {
        for (File parent : ImmutableList.copyOf(parents)) {
            for (String filename : filenames) {
                Iterable<String> filenameVariations = transform.apply(filename);
                for (String filenameVariation : filenameVariations) {
                    File file = new File(parent, filenameVariation);
                    if (isValidResult(file)) {
                        return Optional.of(file);
                    }
                }
            }
        }
        return Optional.absent();
    }
    
    static final class IdentityTransform implements Function<String, Iterable<String>> {

        @Override
        public Iterable<String> apply(String input) {
            return ImmutableList.of(input);
        }
        
        private static final IdentityTransform instance = new IdentityTransform();
        
    }
    
    static final class WindowsTransform extends MultipleSuffixTransform {
        private static final ImmutableList<String> windowsExecutableSuffixes = ImmutableList.of(".exe", ".bat", ".com");

        public WindowsTransform() {
            super(windowsExecutableSuffixes);
        }
        
        private static final WindowsTransform instance = new WindowsTransform();
        
    }
    
    static class MultipleSuffixTransform implements Function<String, Iterable<String>> {

        private final ImmutableList<String> suffixes;

        public MultipleSuffixTransform(Iterable<String> suffixes) {
            this.suffixes = ImmutableList.copyOf(suffixes);
        }
        
        @Override
        public Iterable<String> apply(String filename) {
            String ext = Files.getFileExtension(filename);
            if (ext.isEmpty()) {
                ImmutableList.Builder<String> b = ImmutableList.builder();
                b.add(filename);
                for (String suffix : suffixes) {
                    b.add(filename + suffix);
                }
                return b.build();
            } else {
                return ImmutableList.of(filename);
            }
        }
        
    }
    
    /**
     * Checks whether this instance's predicate is true for a pathname.
     * @param file the file
     * @return true if and only if this instance's predicate returns true
     */
    protected boolean isValidResult(File file) {
        return validPredicate.apply(file);
    }
    
    @VisibleForTesting
    static class Pathnames {
    
        private static final CharMatcher pathSeparatorMatcher = CharMatcher.is(File.pathSeparatorChar);
        private static final Splitter pathListSplitter = Splitter.on(pathSeparatorMatcher).omitEmptyStrings();

        /**
         * Splits a string on the system path separator. Use 
         * {@link Iterables#transform(java.lang.Iterable, com.google.common.base.Function) 
         * Iterables.transform} and the {@link #STRING_TO_FILE} function to 
         * get an iterable over {@code File} objects.
         * @param pathsList
         * @return the constituent paths
         * @see File#pathSeparator
         * 
         */
        public static Iterable<String> splitPaths(String pathsList) {
            return pathListSplitter.split(pathsList);
        }
        
        /**
         * Gets an iterable over the directories enumerated in the {@code PATH}
         * environment variable.
         * @return the iterable of file objects
         */
        static Iterable<File> getSystemPathDirs() {
            return Iterables.transform(splitPaths(System.getenv("PATH")), newFileFunction());
        }

        /**
         * Gets an immutable list containing the directories enumerated in the 
         * {@code PATH} environment variable.
         * @return the list
         */
        static List<File> getSystemPathDirList() {
            return ImmutableList.copyOf(getSystemPathDirs());
        }

        /**
         * Function that constructs files from strings, using the single argument
         * {@code File} constructor.
         */
        static Function<String, File> newFileFunction() {
            return new Function<String, File> () {
                @Override
                public File apply(String input) {
                    return new File(input);
                }
            };
        }

        static boolean hasAnyExtension(@Nullable String pathname) {
            return pathname == null ? false : !Files.getFileExtension(pathname).equals("");
        }
    
    }

    /**
     * Constructs and returns a GNU-style instance that searches the system
     * path for executable files, checking Windows extensions if the platform
     * is Windows.
     * @return a constructed instance
     */
    public static Whicher gnu() {
        return builder()
                .inAny(Pathnames.getSystemPathDirList())
                .executable()
                .detectPlatform()
                .build();
    }
    
    /**
     * Creates a new builder with no parent directories to be searched.
     * Warning: you'll never find anything if you don't add any parents.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a new builder.
     * @param parent the first parent directory to be searched
     * @return the builder
     */
    public static Builder builder(File parent) {
        return new Builder().in(parent);
    }
    
    /**
     * Builder for whicher instances. Provides a fluent way to build 
     * valid whichers.
     */
    public static class Builder {
        
        private final List<File> parents;
        private Predicate<File> validPredicate;
        private Function<String, Iterable<String>> transform = IdentityTransform.instance;
        
        private Builder() {
            parents = new ArrayList<>();
            validPredicate = newIsFilePredicate();
        }
        
        /**
         * Add a directory to be searched.
         * @param parent a directory
         * @return this builder instance
         */
        public Builder in(File parent) {
            parents.add(checkNotNull(parent));
            return this;
        }
        
        /**
         * Add multiple directories to be searched
         * @param parents the directories to search
         * @return this instance
         */
        public Builder inAny(Iterable<File> parents) {
            Iterables.addAll(this.parents, parents);
            return this;
        }
        
        /**
         * Require that the found file satisfies the given predicate, along
         * with all predicates previously added.
         * @param validPredicate the predicate
         * @return this instance
         */
        public Builder check(Predicate<File> validPredicate) {
            this.validPredicate = Predicates.and(this.validPredicate, validPredicate);
            return this;
        }
        
        /**
         * Builds and returns the whicher instance.
         * @return the whicher
         */
        public Whicher build() {
            return new Whicher(parents, validPredicate, transform);
        }
        
        /**
         * Require that the found file be executable.
         * @return this instance
         */
        public Builder executable() {
            return check(newIsExecutablePredicate());
        }
        
        /**
         * Set the transform that maps a single filename to multiple filenames,
         * all of which are to be considered matches. The primary use for
         * a transform is to allow 'foo' to match against 'foo.exe'. 
         * @param transform the transform
         * @return this instance
         */
        public Builder transform(Function<String, Iterable<String>> transform) {
            this.transform = checkNotNull(transform);
            return this;
        }
        
        /**
         * Set transform that maps to filenames with windows executable
         * suffixes. The suffixes are .exe, .bat, and .com.
         * @return this instance
         */
        public Builder windowsSuffixes() {
            return transform(WindowsTransform.instance);
        }
        
        /**
         * Set tranform for Windows suffixes if on Windows, otherwise
         * do nothing.
         * @return this instance
         * @see #windowsSuffixes() 
         */
        public Builder detectPlatform() {
            Platform platform = Platforms.getPlatform();
            if (platform.isWindows()) {
                return windowsSuffixes();
            } else {
                return this;
            }
        }
    }
}