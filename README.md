[![Travis build status](https://img.shields.io/travis/mike10004/common-helper.svg)](https://travis-ci.org/mike10004/common-helper)
[![AppVeyor status](https://ci.appveyor.com/api/projects/status/sklwcp67bi3lpp05?svg=true)](https://ci.appveyor.com/project/mike10004/common-helper)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.mike10004/common-helper.svg)](https://repo1.maven.org/maven2/com/github/mike10004/common-helper/)

Common Helper Libraries
=======================

Libraries that help with some common computing tasks in Java. They are:

* imnetio-helper: imaging, networking, and I/O
* ormlite-helper and ormlite-helper-testtools: help with ORMLite and 
  database unit/integration testing
* native-helper: help with platform-dependent tasks like subprocesses 
  and platform-specific filesystem conventions

## Imaging, Networking, and I/O

### Imaging

Want to get an image's dimensions without buffering all the pixels?

    Dimension dim = ImageInfos.readImageSize(imageBytes);
    System.out.format("%d x %d", dim.width, dim.height);

### Networking 

Want to download something but handle various error conditions (unknown host, 404) 
the same way? Don't try 
[java.net.URLConnection](http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html), 
where you'll get the unknown host exception on `URL.openConnection()`, or if 
you make it past that, have to catch an I/O exception on 4xx and 5xx errors and 
read from the error stream. Just do this instead:

    HttpRequester requester = HttpRequests.newRequester();
    ResponseData responseData = requester.retrieve("http://example.com");
    System.out.println("HTTP response " + responseData.code);
    System.out.println(new String(responseData.data));

### I/O

Want to read from the first good input stream out of multiple potentially 
broken streams?

    ByteSource first = ByteSources.broken();
    byte[] bytes = { (byte) 1, (byte) 2, (byte) 3, (byte) 4};
    ByteSource other = ByteSource.wrap(bytes);
    ByteSource result = ByteSources.or(first, other);
    System.out.println(Arrays.toString(result.toByteArray())); // [1, 2, 3, 4]

## ORMLite helper

Doing some quick database work? This might help you:

    DatabaseContext db = new DefaultDatabaseContext(new H2MemoryConnectionSource());
    try {
        db.getTableUtils().createTable(Customer.class);
        Customer customer = new Customer("Jane Doe", 123);
        db.getDao(Customer.class).create(customer);
    } finally {
        db.closeConnections(false); // 'true' to swallow exception on close
    }

## Native

Want the pathname of the directory where system configuration files are?

    File dir = Platforms.getPlatform().getSystemConfigDir();
    System.out.println(dir); 

...prints `/etc` on Linux and value of `%ProgramData%` environment variable on 
Windows.

Want to launch a subprocess and capture the output as strings?

    // <String, String> parameters refer to type of captured stdout and stderr data
    ProcessResult<String, String> result;
    try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
        result = Subprocess.running("echo")
                .arg("hello, world")
                .build()
                .launcher(processTracker)
                .outputStrings(Charset.defaultCharset())
                .launch()
                .await();
    }
    System.out.println(result.content().stdout()); // prints "hello, world"

# How do I build it?

## Prerequisites

The build system must have Maven 3.0+ installed. On Windows, you must have Perl
installed and `perl.exe` on the system path. Visit 
[strawberryperl.com](http://strawberryperl.com) for free Perl.

On Linux, make sure the **libaio1** (Linux kernel asynchronous I/O access 
library) package is installed, or else the MySQL integration tests will fail.

The MySQL distribution dependency is platform-dependent and Maven central may 
not host an artifact corresponding to your platform. If that is the case, you
can download a [MySQL](http://www.mysql.com) tarball release, re-package it 
into a zip file, and install it to your local repository. For example:

    $ mvn install:install-file -DartifactId=mysql-dist -DgroupId=com.jcabi \
        -Dversion=5.5.41 -Dclassifier=myoperatingsystem-amd64 -Dpackaging=zip \
        -Dfile=/path/to/downloaded/mysql-dist-5.5.41-myoperatingsystem-amd64.zip

Add a profile to your `$HOME/.m2/settings.xml` that only gets activated when your
platform is detected.

## Build

Execute

    $ mvn install

in the parent project directory.
