# WollMux

## Introduction
WollMux is a LibreOffice extension with enhanced template, form, and autotext functionality. It can construct templates on the fly from multiple files (e.g. letterhead, footer, and body text) and will fill in personal and organizational data from various databases such as LDAP. An extra form GUI presents fields in an easily navigable manner and offers plausibility checks and computed values to ease filling in the form. Chainable printing functions allow various transformations during print and custom dialogs.

WollMux is licensed under the [European Union Public Licence (EUPL)](https://joinup.ec.europa.eu/licence/european-union-public-licence-version-11-or-later-eupl).

**More information about WollMux can be found at our main page at [wollmux.org](https://wollmux.org/)**

## How to build?
WollMux is separated into 3 modules:
* wollmux: All main classes of the extension
* [wollmux-interfaces](idl/): UNO API Interfaces
* [unohelper](https://github.com/LibreOffice/UNOHelper): Support classes for using UNO

The following applications have to be installed to compile WollMux:
* JAVA Development Kit (e.g. [Adoptium](https://adoptium.net)) (at least Java 11)
* [Apache Maven](https://maven.apache.org/download.cgi)
* [Git](http://git-scm.com/downloads/)
* [LibreOffice SDK](https://api.libreoffice.org/docs/install.html)

Afterwards the property `UNO_PATH` has to be set in the maven settings.
* open `~\.m2\settings.xml`
* add a profile

```
<profile>
  <id>UNO</id>
  <activation>
    <activeByDefault>true</activeByDefault>
  </activation>
  <properties>
    <UNO_PATH>/opt/libreoffice/program</UNO_PATH>
  </properties>
</profile>
```
* `<UNO_PATH>` is the folder, where the LibreOffice executable is located (eg. /opt/libreoffice/program)

Before building WollMux, you need to install UNOHelper to your local maven repo (the prebuilt package is currently not available):

```
git clone https://github.com/LibreOffice/UNOHelper.git
mvn install
```

Then run the following commands to download and build WollMux:

```
git clone https://github.com/LibreOffice/WollMux.git
mvn clean package
```

The compiled extension can be found at oxt/target/WollMux.oxt

### Build errors
* **There are files with header to update**: Some of the source files don't have a license header. The header can be updated with:

```
mvn license:update-file-header
```

## Translations

<a href="https://translations.documentfoundation.org/engage/wollmux/">
<img src="https://translations.documentfoundation.org/widgets/wollmux/-/wollmux/multi-auto.svg" alt="Translation status" />
</a>

[Translate WollMux](https://translations.documentfoundation.org/engage/wollmux)

## Debugging
### External WollMux (Eclipse)
WollMux.oxt extension is not installed in LibreOffice, but is loaded from external by starting a debug session in eclipse. There exist an additional extension **WollMux_ButtonsOnly.oxt**, which only contains the toolbars and dialogs. This extension must be installed in LibreOffice. Therefore call

```
mvn -P ButtonsOnly generate-sources
```

and the extension is build and installed, if the program `unopkg` is availble. Otherwise you have to manually install the extension, which can be found at oxt/target/WollMux_ButtonsOnly.oxt.

Activate the maven profile "development" in Project > Properties > Maven > Active Maven Profiles.

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

There are also some configuration changes for LibreOffice necessary. Go to **Tools** &rarr; **Options...** &rarr; **LibreOffice** &rarr; **Advanced** &rarr; **Parameters...** and add the following parameters:
* `-Xdebug`
* `-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n`

If suspend is set to `y`, LibreOffice waits until an external process connects before initializing.

## Translations

To update pot/po files from source, run these commands:

```bash
xgettext --default-domain=wollmux --output=core/i18n/wollmux.pot --language=java --from-code=UTF-8 --keyword --keyword=m $(find . -name "*.java")
for i in core/i18n/*.po; do msgmerge -U $i core/i18n/wollmux.pot; done
```

This creates the `core/i18n/wollmux.pot` template file. Translate it and place the translated po files in `core/i18n` folder. The build system will pick up the po files, convert and bundle them with WollMux.
