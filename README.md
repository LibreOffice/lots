# WollMux

## Introduction
The WollMux is a LibreOffice plugin with enhanced template, form, and autotext functionality. It can construct templates on the fly from multiple files (e.g. letterhead, footer, and body text) and will fill in personal and organizational data from various databases such as LDAP. An extra form GUI presents fields in an easily navigable manner and offers plausibility checks and computed values to ease filling in the form. Chainable printing functions allow various transformations during print and custom dialogs.

WollMux is licensed under the European Union Public Licence (EUPL - http://joinup.ec.europa.eu/software/page/eupl).

**More information about WollMux can be found at our main page at http://www.wollmux.org**

## How to build?
WollMux is separated into 3 modules:
* wollmux: all main classes of the extension
* [wollmux-interfaces](idl/README.md): interfaces of the UNO API
* [unohelper](https://github.com/WollMux/UNOHelper): Support classes for using UNO

The following applications have to be installed to compile WollMux:
* [JAVA JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Apache Maven](https://maven.apache.org/download.cgi)
* [Git](http://git-scm.com/downloads/)
* [LibreOffice SDK](https://api.libreoffice.org/docs/install.html)

Perform the following commands to download the sources and build the LibreOffice extension. Special dependencies of WollMux are hosted at
[Bintray](https://bintray.com/wollmux/WollMux), which is already configured as maven repository in pom.xml

```
git clone https://github.com/WollMux/WollMux.git
git checkout WollMux_18.2
mvn clean package
```

The compiled extension can be found at dist/WollMux.oxt

### Build errors
* **There are files with header to update**: Some of the source files don't have a license header. The header can be updated with:

```
mvn license:update-file-header
```

## Debugging
### External WollMux (Eclipse)
WollMux.oxt extension is not installed in LibreOffice, but is loaded from external by starting a debug session in eclipse. There exist an additional extension **WollMux_ButtonsOnly.oxt**, which only contains the toolbars and dialogs. This extension must be installed in LibreOffice. Therefore call

```
mvn -P ButtonsOnly generate-sources
```

and the extension is build and installed, if the program `unopkg` is availble. Otherwise you have to manually install the extension, which can be found at dist/WollMux_ButtonsOnly.oxt.

Configure a debug-configuration of type "Java Application" with main class **de.muenchen.allg.itd51.wollmux.DebugExternalWollMux**. Add a user defined library to the classpath. The library must contain these jars of LibreOffice which can be found at <path_to_LibreOffice>/program/classes:
* java_uno.jar
* juh.jar
* jurt.jar
* ridl.jar
* unoloader.jar
* unoil.jar

Make sure, that there is no running LibreOffice process before starting debugging.

Advantages:
* Hot code replacement

Disadvantages:
* different startup behaviour

### Remote Debugging
If you have a running instance, which you like to debug. You can connect to it by another Eclipse debug-configuration:
* type: Remote java Application
* connection type: Standard (Socket Attach)
* connection properties: localhost, 8000

There are also some configuration changes for LibreOffice necessary. Go to **Extras** &rarr; **Options...** &rarr; **LibreOffice** &rarr; **Additional** &rarr; **Parameter...** and add the following parameters:
* `-Xdebug`
* `-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n`

If suspend is set to `y`, LibreOffice waits until an external process connects before initializing.
