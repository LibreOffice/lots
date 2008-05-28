@echo off
set "JAR_FILE=<Path to>\WollMuxBar.jar"
                              
set JAVA_HOME=C:\Programme\Java\<Java Version>
set PATH=%JAVA_HOME%\bin;%PATH%

start "WollMuxBar" javaw -jar "%JAR_FILE%" %1 %2 %3 %4 %5 %6 %7 %8
