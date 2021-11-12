# testIDEA
This application is unit testing tool for embedded systems. Current implementation uses hardware debuggers from [iSYSTEM](https://www.isystem.com/).

__Main features:__

- testIDEA is interactive editor for creating and running tests. 
- Test cases and test vectors are __executed on the real hardware or 
  simulator without code instrumentation__.
- The major advantage of non-instrumented unit tests is the fast 
  turn-around-times, because the tests can run without compilation, 
  linking and download. 
- No test drivers are needed and therefore no additional resources 
  are used on the target system. 
- Tracing, profiling and code coverage analysis can be part of test cases. 
- Reports with detailed information about test execution and results. 
- YAML format is used to stores test cases.
- Test specification files can be easily edited with any text editor and 
  later used either by scripts or testIDEA. 
- Tests created with testIDEA can be easily used in scripts via isystem.test 
  API. 
- Integration with isystem.connect API expands the operation area of iSYSTEM 
  test technology from unit testing to integration and system testing. 
- Python scripts to run tests in headless mode can be generated.
- Code completion of function and variable names.
- Debugging of test cases with step execution is possible.

![Screenshot](main-window.png)


# Using prebuilt testIDEA

It is possible do download prebuilt testIDEA as part of iSYSTEM's winIDEA
from [iSYSTEM web page](https://www.isystem.com/downloads/winidea.html). 
If you want to build it yourself, follow instructions below.

# Building

testIDEA is Java application based on Eclipse, which can be built on Linux 
or Windows. For communication with debugger
hardware it uses native library, which is part of this repository in compiled 
form. You can also download them as part of 
[isystem.connect SDK](https://www.isystem.com/downloads/isystem-connect-sdk.html).

Required tools:
- JDK 11
- Gradle
- Maven

Env. variable `JAVA_HOME` must be defined, and `gradle` and `mvn` must be 
in `PATH`.

Clone this repository, then:

    $ cd testIDEA

To build standalone application:

    $ gradle exeBuildTest   

To build Eclipse plug-in:

    $ gradle pluginBuildTest

