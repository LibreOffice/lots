@ECHO OFF
setlocal enableextensions

REM This batch script emulates the functionality of the makeversion bash script.
REM The script uses some batch functionality introduced with Windows XP so it probably doesn't work on Windows 2000.

REM First check if there are enough command line arguments, else exit
set argumentcount=0
for %%x in (%*) do set /A argumentcount+=1
if %argumentcount% neq 4 (
echo USAGE: makeversion ^<versionfile^> ^<buildinfofile^> ^<descriptionxmlfile^> ^<updatexmlfile^> 1>&2
echo If ^<versionfile^> exists and contains a version number that does not 1>&2
echo contain 'rev' the version number from ^<versionfile^> is used. 1>&2
exit /b 1
)

REM Read command line arguments
set versionfile=%1
set buildinfofile=%2
set descriptionxmlfile=%3
set updatexmlfile=%4

REM Check if hg repository is present
hg identify 2>nul 1>&2
if %ERRORLEVEL% neq 0 (
echo Getting HG version info failed =^> Falling back to shipped version info 1>&2
exit /b 1
)

REM Get date of last commit and determine version numbers
REM First number = year - 2000; Second number = month
for /f "delims=- tokens=1-2" %%a in (
'hg log -l1 --template "{date|shortdate}"'
) do (
set /a v1=%%a - 2000
set /a v2=%%b
)

REM Get local revision number of last commit
for /f "tokens=*" %%a in (
'hg log -l1 --template="{rev}"'
) do (
set localrev=%%a
)

REM Get full changeset identification hash of last commit
for /f "tokens=*" %%a in (
'hg log -l1 --template="{node}"'
) do (
set rev=%%a
)

REM Set version variable using the numbers and local revision determined above
set version=%v1%.%v2%rev%localrev%

REM Check if <versionfile> contains "rev", if not we use the version number from <versionfile>
for /f "delims=" %%a in (%versionfile%) do (
set versionfilecontent=%%a
)
echo %versionfilecontent% | findstr "rev" >nul
if %ERRORLEVEL% NEQ 0 set version=%versionfilecontent%

REM create version and buildinfo files
echo %version% >%versionfile%
echo Version: %version%, Revision: %rev% >%buildinfofile%

REM create description.xml file
echo ^<?xml version="1.0" encoding="UTF-8"?^> >%descriptionxmlfile%
echo ^<description xmlns="http://openoffice.org/extensions/description/2006" >>%descriptionxmlfile%
echo	     xmlns:xlink="http://www.w3.org/1999/xlink"^> >>%descriptionxmlfile%
echo	^<version value="%version%" /^> >>%descriptionxmlfile%
echo	^<identifier value="de.muenchen.allg.d101.wollmux"/^> >>%descriptionxmlfile%
echo	^<dependencies^> >>%descriptionxmlfile%
echo		^<OpenOffice.org-minimal-version value="2.1" name="OpenOffice.org 2.1"/^> >>%descriptionxmlfile%
echo	^</dependencies^> >>%descriptionxmlfile%
echo. >>%descriptionxmlfile%
echo	^<publisher^> >>%descriptionxmlfile%
echo		^<name xlink:href="http://www.wollmux.org" lang="de"^>WollMux.org^</name^> >>%descriptionxmlfile%
echo	^</publisher^> >>%descriptionxmlfile%
echo. >>%descriptionxmlfile%
echo	^<display-name^> >>%descriptionxmlfile%
echo		^<name lang="de-DE"^>WollMux^</name^> >>%descriptionxmlfile%
echo	^</display-name^> >>%descriptionxmlfile%
echo. >>%descriptionxmlfile%
echo	^<update-information^> >>%descriptionxmlfile%
echo	  ^<src xlink:href="http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/packages/WollMux-snapshot/WollMux.update.xml"/^> >>%descriptionxmlfile%
echo	^</update-information^> >>%descriptionxmlfile%
echo ^</description^> >>%descriptionxmlfile%

REM Create update.xml file
echo ^<?xml version="1.0" encoding="UTF-8"?^> >%updatexmlfile%
echo ^<description xmlns="http://openoffice.org/extensions/update/2006" >>%updatexmlfile%
echo	     xmlns:xlink="http://www.w3.org/1999/xlink"^> >>%updatexmlfile%
echo	^<version value="%version%" /^> >>%updatexmlfile%
echo	^<identifier value="de.muenchen.allg.d101.wollmux"/^> >>%updatexmlfile%
echo	^<update-download^> >>%updatexmlfile%
echo          ^<src xlink:href="http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/packages/WollMux-snapshot/WollMux.oxt"/^> >>%updatexmlfile%
echo	^</update-download^> >>%updatexmlfile%
echo ^</description^> >>%updatexmlfile%

endlocal