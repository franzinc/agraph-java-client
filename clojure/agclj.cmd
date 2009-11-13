@echo on
@setlocal

@echo off
set CLASSPATH=%CLASSPATH%;lib\clojure-1.0.jar
set CLASSPATH=%CLASSPATH%;lib\clojure-contrib-1.0.jar
set CLASSPATH=%CLASSPATH%;..\agraph-3.2.jar
set CLASSPATH=%CLASSPATH%;..\lib\openrdf-sesame-2.2-onejar.jar
set CLASSPATH=%CLASSPATH%;..\lib\slf4j-api-1.4.3.jar
set CLASSPATH=%CLASSPATH%;..\lib\slf4j-log4j12-1.4.3.jar
set CLASSPATH=%CLASSPATH%;..\lib\log4j-1.2.12.jar

@REM clojure can load src directly, so not using AOT-compilation here
set CLASSPATH=%CLASSPATH%;src
set CLASSPATH=%CLASSPATH%;test
set CLASSPATH=%CLASSPATH%;tutorial

@echo %CLASSPATH%
@echo on

java clojure.main

@REM http://bc.tech.coop/blog/081023.html for debugging with jswat: -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8888
@REM http://java.sun.com/j2se/1.5.0/docs/tooldocs/share/jconsole.html: -Dcom.sun.management.jmxremote=true

@endlocal
