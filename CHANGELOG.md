common-helper Changelog
=======================

9.0.0
-----

* remove **native-helper-legacy** module
* native-helper: use Guava ThreadFactoryBuilder instead of custom ThreadFactory
* add some junit tests
* improve javadoc
* native-helper: fix Platforms bug relating to Windows 2012

8.1.0
-----

* raise ormlite dependency version to 5.1

8.0.5
-----

* pom/dependency maintenance

8.0.4
-----

* fix appveyor build
* add HttpRequests.ResponseData builder() method

8.0.3
-----

* imnetio-helper: make HttpRequests.ResponseData.getFirstHeaderValue match case-insensitively

8.0.2
-----

* native-helper-legacy: revert to Ant-based implementation

8.0.0
-----

* native-helper: introduce Subprocess API for executing processes
