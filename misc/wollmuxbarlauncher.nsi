# WollMuxBar Launcher
# -------------------
# This is the Nullsoft Scriptable Install System (NSIS) script for generating
# the wollmuxbar.exe which is used for launching WollMuxBar.jar.
# The file WollMuxBar.jar must be in the same directory as wollmuxbar.exe for
# the launcher to work. The launcher automatically searches for a Java
# installation (see documentation of GetJRE function below).
# 
# Copyright (c) 2011 Landeshauptstadt München
# Author: Daniel Benkmann
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the European Union Public Licence (EUPL), 
# version 1.0 (or any later version).
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# European Union Public Licence for more details.
# 
# You should have received a copy of the European Union Public Licence
# along with this program. If not, see 
# http://www.osor.eu/eupl

!ifndef OUT_FILE ;; name of the outfile
	!define OUT_FILE "wollmuxbar.exe" ;; default
!endif

!ifndef JAR_FILE ;; name of the jar file to launch
	!define JAR_FILE "WollMuxBar.jar" ;; default
!endif


Name "WollMuxBar Launcher"
Caption "WollMuxBar Launcher"
Icon "wollmux.ico"
OutFile "${OUT_FILE}"

!if ${NSIS_VERSION} != "v2.19-3"
	RequestExecutionLevel user ;; needed to set ExecutionLevel for Vista/Windows 7 - available since NSIS ver. 2.21
!endif

SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow


!include FileFunc.nsh
!insertmacro GetParameters


Section ""
  Call GetJRE
  Pop $R0
  StrCmp $R0 "NOTFOUND" 0 +2
    MessageBox MB_OK|MB_ICONEXCLAMATION "Could not execute WollMuxBar because no Java executable was found!"
  
  ;; get command line parameters
  ${GetParameters} $R1
  
  StrCpy $0 '"$R0" -Xrs -jar "${JAR_FILE}" $R1'
 
  SetOutPath $EXEDIR
  Exec $0
SectionEnd
 
Function GetJRE
;
;  returns the full path of a valid java.exe (or javaw.exe - depending on how JAVAEXE is defined)
;  looks in:
;  1 - .\jre directory (JRE Installed with application)
;  2 - the registry value "JavaHome" of the key "HKCU\Software\WollMux"
;  3 - the registry value "JavaHome" of the key "HKLM\Software\WollMux"
;  4 - JAVA_HOME environment variable
;  5 - the registry value set by the Java Installation in HKLM
; 
;  If nothing was found the string "NOTFOUND" is returned
 
  Push $R0
  Push $R1
 
  ; use javaw.exe to avoid dosbox.
  ; use java.exe to keep stdout/stderr
  !define JAVAEXE "javaw.exe"
 
 ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 1) found it locally
  StrCpy $R0 ""
 
  ClearErrors
  ReadRegStr $R0 HKCU "Software\WollMux" "JavaHome"
  IfErrors +4
  StrCmp $R0 "" +4  ;; check if JavaHome entry is empty
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 2) found it in the HKCU WollMux registry key
  StrCpy $R0 ""
  
  ClearErrors
  ReadRegStr $R0 HKLM "SOFTWARE\WollMux" "JavaHome"
  IfErrors +4
  StrCmp $R0 "" +4  ;; check if JavaHome entry is empty
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 3) found it in the HKLM WollMux registry key
  StrCpy $R0 ""
 
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  IfErrors +3
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 4) found it in JAVA_HOME
  StrCpy $R0 ""
 
  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  IfErrors +3
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 5) found it in the registry key set by Java
  
  StrCpy $R0 "NOTFOUND"  ;; 6) return "NOTFOUND"
 
 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd