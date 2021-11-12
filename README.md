# testIDEA
This application is unit testing tool for embedded systems. Current implementation uses hardware debuggers from [iSYSTEM](https://www.isystem.com/).

__Main features:__

- testIDEA is interactive editor for creating and running tests. 
- Test cases and test vectors are __executed on the real hardware or simulator without code instrumentation__ - other tools usually instrument the code. 
- The major advantage of non-instrumented unit tests is the fast turn-around-times, because the tests can run without compilation, linking and download. 
- No test drivers are needed and therefore no additional resources are used on the target system. 
- Tracing, profiling and code coverage analysis can be part of test cases. 
- It creates reports with detailed information about test execution and results. 
- testIDEA stores the test cases in YAML format.
- Test specification files can be easily edited with any text editor and later used either by scripts or testIDEA. 
- Tests created with testIDEA can be easily used in scripts via isystem.test API. 
- Integration with isystem.connect API expands the operation area of iSYSTEM test technology from unit testing to integration and system testing. 

# Building
Required tools:
- JDK 11
- Gradle

Clone this repository, then:

    $ cd testIDEA

To build standalone application:

    $ gradle exeBuildTest   

To build Eclipse plug-in:

    $ gradle pluginBuildTest
