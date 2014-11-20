TUIO_Mouse
==========

Installation and Logging Set Up
-------------------------------

Fetch the latest [.zip file] [1] from github, and unzip it wherever the
program should lie on the destination host.

Logging to the Windows Event Viewer is provided by [log4jna] [2].

Please follow the instructions provided by that project about [creating
a key in the registry] [3].

[1]: https://github.com/andrewburnheimer/TUIO_Mouse/archive/master.zip

[2]: https://github.com/dblock/log4jna

[3]: https://github.com/dblock/log4jna/blob/master/doc/org.apache.log4jna.nt.Win32EventLogAppender.md#registry


Usage
-----

    java -jar bin/TuioMouse.jar

More details about usage can be found with:

    java -jar bin/TuioMouse.jar -h


Logging to the Console
----------------------

Increase log verbosity, and add an appender to get debugging info
directly on the console:

    log4j.rootCategory=DEBUG, A, A1

    TuioMouse=DEBUG

		. . . appender.A configs . . .

    log4j.appender.A1=org.apache.log4j.ConsoleAppender
    
    # A1 uses PatternLayout.
    log4j.appender.A1.layout=org.apache.log4j.PatternLayout
    log4j.appender.A1.layout.ConversionPattern=%r %5p [%t] (%F:%L) - %m%n


Contributing
------------

Build with the following commands:

    $ javac -Xlint:unchecked -O -source 1.5 -target 1.5 -cp ./lib/platform.jar:./lib/jna.jar:./lib/log4jna.jar:./lib/log4j-1.2.17.jar:./lib/libTUIO.jar:./lib/commons-cli-1.2.jar TuioMouse.java
    $ jar cfm bin/TuioMouse.jar mouseManifest.inc TuioMouse.class
