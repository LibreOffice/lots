# WollMux Interfaces

This is the specification of services provided by the extension WollMux.

## How to build?
To build the specification the [LibreOffice SDK](https://api.libreoffice.org/docs/install.html) has to be installed. With the following commands the Interfaces are build:

```
UNO_PATH=<uno_path> mvn clean package
```

`<uno_path>` is the folder, where the LibreOffice executable is located (eg. /opt/libreoffice/program)

If you like to install the maven artifact in your local repository for testing changes you have to call

```
UNO_PATH=<uno_path> mvn clean install
```
