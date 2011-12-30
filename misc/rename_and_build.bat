@ECHO OFF
setlocal enableextensions

REM Verwendung des Skripts:
REM rename_and_build <neuerName> <Ant-Target(s)>
REM Beispiel:
REM rename_and_build SuperOfficeTool clean all wininstaller
REM Wenn <neuerName> Leerzeichen enthält (was nicht empfohlen wird), dann muss er in Anführungszeichen gesetzt werden.
REM <Ant-Target(s)> ist eine Auflistung der Ant-Targets, die für den WollMux-Build ausgeführt werden sollen.
REM Das Ant-Target "wollmuxbar.exe" wird auf jeden Fall immer ausgeführt, damit eine entsprechend neu benannte WollMux-Executable generiert wird.

REM Dieses Skript kann leider nicht alles automatisiert umbenennen.
REM Folgende Schritte sind zum Umbenennen des WollMux noch manuell nötig und müssen durchgeführt werden bevor dieses Skript ausgeführt wird:
REM 1. misc/makeversion.bat anpassen:
REM    - Im unteren Teil, wo die description.xml generiert wird, müssen der <display-name> sowie der <publisher> angepasst werden.
REM    - Ebenfalls in der description.xml sollte der <update-information>-Teil am besten ganz auskommentiert/gelöscht werden
REM    - Die update.xml ist ohnehin uninteressant, deswegen wäre eine Änderung daran überflüssig.
REM 2. oxt/WriterWindowState.xcu anpassen:
REM    - Einfach jedes Vorkommen von "WollMux" entsprechend ersetzen (Vorsicht: NICHT das kleingeschriebene "wollmux" ersetzen!)
REM 3. oxt/help/component*.txt nach Bedarf anpassen. Hier steht die (lokalisierte) Kurzbeschreibung, die im Extension Manager angezeigt wird.
REM 4. oxt/META-INF/manifest.xml anpassen:
REM    - Die Vorkommen von "WollMux.uno.jar" und "WollMux.rdb" entsprechend anpassen
REM    - ACHTUNG: Vorkommen von "basic/WollMux/" NICHT anpassen!
REM 5. In der eingesetzten WollMux-Konfig die Strings entsprechend anpassen, insbesondere die für GUI-Elemente.
REM Für eine ausführlichere Anleitung siehe auch http://www.wollmux.net/wiki/Rebranding


REM First check if there are enough command line arguments, else exit
set argumentcount=0
for %%x in (%*) do set /A argumentcount+=1
if %argumentcount% lss 2 (
echo USAGE: rename_and_build ^<name^> ^<ant target^(s^)^> 1>&2
echo Note: A maximum of 8 ant targets are supported. 1>&2
echo Note: The ant target "wollmuxbar.exe" will always be executed. 1>&2
exit /b 1
)

REM Read command line arguments
set wollmuxname=%~1
set anttargets=%~2 %~3 %~4 %~5 %~6 %~7 %~8 %~9

REM Change to WollMux directory (assuming this script lies in WollMux\misc)
cd "%~dp0"
cd ..

REM Backup WollMux src directory
xcopy /e /i src src_backup

REM Compile and start Java program to replace occurences of "WollMux" in source code with new name
cd misc
javac WollMuxStringReplacer.java
java WollMuxStringReplacer "WollMux" "%wollmuxname%"
del WollMuxStringReplacer.class
cd ..

REM Call WollMux ant script with replaced names (fine-tune this line to your needs)
call ant -DCOMPONENT="%wollmuxname%" -DWOLLMUXBAR_NAME="%wollmuxname%" -DWOLLMUXBAR_JAR_NAME="%wollmuxname%.jar" -DWOLLMUXBAR_EXE_NAME="%wollmuxname%.exe" wollmuxbar.exe %anttargets%

REM Restore old src directory
rmdir /s /q src
move src_backup src
cd misc

endlocal