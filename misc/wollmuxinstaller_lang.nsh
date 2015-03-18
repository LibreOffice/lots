# This file contains all language specific settings and strings used by the WollMux installer.
# It is included with !include by wollmuxinstaller.nsi.

# NSIS MUI Language table
LangString MUI_TEXT_WELCOME_INFO_TITLE ${LANG_ENGLISH} "WollMux"
LangString MUI_TEXT_WELCOME_INFO_TITLE ${LANG_GERMAN} "WollMux"

LangString MUI_TEXT_WELCOME_INFO_TEXT ${LANG_ENGLISH} \
"WollMux adds functions to LibreOffice respectivly OpenOffice.org, to make the handling of templates, forms and letterheads much easier.$\n$\n\
Installation Requirements:$\n- Java Runtime Environment 32-bit (JRE)$\n- LibreOffice / OpenOffice.org $\n$\n\
The installer will now try to close any open office windows.$\n\
This can take a few seconds."

LangString MUI_TEXT_WELCOME_INFO_TEXT ${LANG_GERMAN} \
"Der WollMux erweitert LibreOffice bzw. OpenOffice.org um zahlreiche Funktionen, welche die Arbeit mit Vorlagen, Formularen und Briefköpfen wesentlich erleichtern.$\n$\n\
Installationsanforderungen:$\n- Java-Laufzeitumgebung 32-Bit (JRE)$\n- LibreOffice / OpenOffice.org $\n$\n\
Der Installer versucht nun offene Office-Fenster zu schließen.$\n\
Dieser Vorgang kann einige Sekunden dauern."

LangString MUI_TEXT_COMPONENTS_TITLE ${LANG_ENGLISH} "${WOLLMUX}"
LangString MUI_TEXT_COMPONENTS_TITLE ${LANG_GERMAN} "${WOLLMUX}"

LangString MUI_TEXT_COMPONENTS_SUBTITLE ${LANG_ENGLISH} "${VERSION}"
LangString MUI_TEXT_COMPONENTS_SUBTITLE ${LANG_GERMAN} "${VERSION}"

#LangString MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE ${LANG_ENGLISH} ""
#LangString MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE ${LANG_GERMAN} ""

#LangString MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO ${LANG_ENGLISH} ""
#LangString MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO ${LANG_GERMAN} ""

LangString MUI_TEXT_DIRECTORY_TITLE ${LANG_ENGLISH} "${WOLLMUX}"
LangString MUI_TEXT_DIRECTORY_TITLE ${LANG_GERMAN} "${WOLLMUX}"

LangString MUI_TEXT_DIRECTORY_SUBTITLE ${LANG_ENGLISH} "${VERSION}"
LangString MUI_TEXT_DIRECTORY_SUBTITLE ${LANG_GERMAN} "${VERSION}"

LangString MUI_TEXT_INSTALLING_TITLE ${LANG_ENGLISH} "${WOLLMUX}"
LangString MUI_TEXT_INSTALLING_TITLE ${LANG_GERMAN} "${WOLLMUX}"

LangString MUI_TEXT_INSTALLING_SUBTITLE ${LANG_ENGLISH} "${VERSION}"
LangString MUI_TEXT_INSTALLING_SUBTITLE ${LANG_GERMAN} "${VERSION}"

LangString MUI_TEXT_FINISH_TITLE ${LANG_ENGLISH} "${WOLLMUX}"
LangString MUI_TEXT_FINISH_TITLE ${LANG_GERMAN} "${WOLLMUX}"

LangString MUI_TEXT_FINISH_SUBTITLE ${LANG_ENGLISH} "${VERSION}"
LangString MUI_TEXT_FINISH_SUBTITLE ${LANG_GERMAN} "${VERSION}"

#LangString MUI_TEXT_ABORT_TITLE ${LANG_ENGLISH} ""
#LangString MUI_TEXT_ABORT_TITLE ${LANG_GERMAN} ""

#LangString MUI_TEXT_ABORT_SUBTITLE ${LANG_ENGLISH} ""
#LangString MUI_TEXT_ABORT_SUBTITLE ${LANG_GERMAN} ""

#LangString MUI_UNTEXT_CONFIRM_TITLE ${LANG_ENGLISH} ""
#LangString MUI_UNTEXT_CONFIRM_TITLE ${LANG_GERMAN} ""

#LangString MUI_UNTEXT_CONFIRM_SUBTITLE ${LANG_ENGLISH} ""
#LangString MUI_UNTEXT_CONFIRM_SUBTITLE ${LANG_GERMAN} ""

LangString MUI_UNTEXT_UNINSTALLING_TITLE ${LANG_ENGLISH} "${WOLLMUX}"
LangString MUI_UNTEXT_UNINSTALLING_TITLE ${LANG_GERMAN} "${WOLLMUX}"

LangString MUI_UNTEXT_UNINSTALLING_SUBTITLE ${LANG_ENGLISH} "${VERSION}"
LangString MUI_UNTEXT_UNINSTALLING_SUBTITLE ${LANG_GERMAN} "${VERSION}"

#LangString MUI_UNTEXT_FINISH_TITLE ${LANG_ENGLISH} ""
#LangString MUI_UNTEXT_FINISH_TITLE ${LANG_GERMAN} ""

#LangString MUI_UNTEXT_FINISH_SUBTITLE ${LANG_ENGLISH} ""
#LangString MUI_UNTEXT_FINISH_SUBTITLE ${LANG_GERMAN} ""

#LangString MUI_UNTEXT_ABORT_TITLE ${LANG_ENGLISH} ""
#LangString MUI_UNTEXT_ABORT_TITLE ${LANG_GERMAN} ""

#LangString MUI_UNTEXT_ABORT_SUBTITLE ${LANG_ENGLISH} ""
#LangString MUI_UNTEXT_ABORT_SUBTITLE ${LANG_GERMAN} ""


# Section Names
LangString StartMenuShortcut ${LANG_ENGLISH} "Start menu shortcut"
LangString StartMenuShortcut ${LANG_GERMAN} "Startmenü-Eintrag"
LangString DesktopShortcut ${LANG_ENGLISH} "Desktop shortcut"
LangString DesktopShortcut ${LANG_GERMAN} "Desktop-Verknüpfung"

# Start menu link names
LangString UninstallWollMux ${LANG_ENGLISH} "Uninstall ${WOLLMUX}"
LangString UninstallWollMux ${LANG_GERMAN} "${WOLLMUX} entfernen"

# General Messages
LangString InstallationRequirements ${LANG_ENGLISH} "Installation Requirements:$\n- Java Runtime Environment 32-bit (JRE) is installed$\n- LibreOffice/OpenOffice is installed"
LangString InstallationRequirements ${LANG_GERMAN} "Installationsanforderungen:$\n- Java-Laufzeitumgebung 32-Bit (JRE) ist installiert$\n- LibreOffice/OpenOffice ist installiert"

LangString NeedAdminMessage ${LANG_ENGLISH} "You need administrator privileges to execute this program!"
LangString NeedAdminMessage ${LANG_GERMAN} "Sie benötigen Administrator-Rechte um dieses Programm auszuführen!"

LangString TryToKillOOoMessage ${LANG_ENGLISH} \
"LibreOffice/OpenOffice must be closed before execution of this installer. $\n\
The installer will now try to close any open office windows. $\n\
This can take a few seconds."
LangString TryToKillOOoMessage ${LANG_GERMAN} \
"LibreOffice/OpenOffice muss vor dem Ausführen dieses Installers beendet werden.$\n\
Der Installer versucht nun offene Office-Fenster zu schließen.$\n\
Dieser Vorgang kann einige Sekunden dauern."

LangString OOoRunningMessage ${LANG_ENGLISH} \
"LibreOffice/OpenOffice or the quickstarter could not be closed!$\n$\n\
Please close all office windows and the quickstarter, then try again.$\n$\n\
Another reason for this error could be that LibreOffice/OpenOffice is not installed on your system."
LangString OOoRunningMessage ${LANG_GERMAN} \
"LibreOffice/OpenOffice bzw. dessen Schnellstarter konnte nicht beendet werden!$\n$\n\
Bitte schließen Sie alle offenen Office-Fenster sowie den Schnellstarter und versuchen Sie es erneut.$\n$\n\
Ein anderer Grund für diesen Fehler könnte sein, dass LibreOffice/OpenOffice nicht auf Ihrem System installiert ist."

LangString NoJavaFoundMessage ${LANG_ENGLISH} \
"No Java (32-bit) was found!$\n\
Please install the Java Runtime Environment (32-bit).$\n$\n\
If the JRE is already installed make sure the JAVA_HOME environment variable contains the path to your JRE directory."
LangString NoJavaFoundMessage ${LANG_GERMAN} \
"Es konnte kein Java (32-Bit) gefunden werden!$\n\
Bitte installieren Sie Java (32-Bit) auf Ihrem System.$\n$\n\
Wenn eine Java-Laufzeitumgebung (JRE) bereits installiert ist, stellen Sie sicher, dass die JAVA_HOME Umgebungsvariable den Pfad zu ihrem JRE-Ordner enthält."

LangString AbortMessage ${LANG_ENGLISH} "Execution aborted!"
LangString AbortMessage ${LANG_GERMAN} "Ausführung abgebrochen!"

# Installer Messages
LangString NoOOoFoundMessage ${LANG_ENGLISH} \
"Could not install ${WOLLMUX}.oxt because no LibreOffice/OpenOffice installation was found!$\n$\n\
Please install ${WOLLMUX}.oxt manually using the LibreOffice/OpenOffice extension manager."
LangString NoOOoFoundMessage ${LANG_GERMAN} \
"Konnte ${WOLLMUX}.oxt nicht installieren, da keine LibreOffice/OpenOffice-Installation gefunden wurde!$\n$\n\
Bitte installieren Sie die ${WOLLMUX}.oxt manuell mit dem Extension Manager von LibreOffice/OpenOffice."

LangString UnoPkgRemoveMessage ${LANG_ENGLISH} "Removing previously installed ${WOLLMUX} extensions..."
LangString UnoPkgRemoveMessage ${LANG_GERMAN} "Entferne bereits installierte ${WOLLMUX}-Extensions..."

LangString UnoPkgAddMessage ${LANG_ENGLISH} "Installing ${WOLLMUX} extension into LibreOffice/OpenOffice..."
LangString UnoPkgAddMessage ${LANG_GERMAN} "Füge ${WOLLMUX}-Extension zu LibreOffice/OpenOffice hinzu..."

LangString UnoPkgErrorMessage ${LANG_ENGLISH} \
"Error while installing ${WOLLMUX}.oxt extension into LibreOffice/OpenOffice!$\n$\n\
Please make sure no process with the name 'soffice.bin' is running and that you have the necessary rights, then try again."
LangString UnoPkgErrorMessage ${LANG_GERMAN} \
"Fehler beim Installieren der ${WOLLMUX}.oxt-Extension in LibreOffice/OpenOffice!$\n$\n\
Bitte stellen Sie sicher, dass kein Prozess mit dem Namen 'soffice.bin' läuft und dass sie die notwendigen Benutzerrechte haben."

LangString AbortUnoPkgErrorMessage ${LANG_ENGLISH} \
"Installation was aborted because ${WOLLMUX}.oxt extension couldn't be installed!$\n$\n\
No files were created during installation but an already existing ${WOLLMUX} extension may have been uninstalled."
LangString AbortUnoPkgErrorMessage ${LANG_GERMAN} \
"Installation wurde abgebrochen, da die ${WOLLMUX}.oxt-Extension nicht installiert werden konnte!$\n$\n\
Keine Dateien wurden während der Installation erzeugt, aber eventuell wurde eine bereits vorhandene ${WOLLMUX}-Extension deinstalliert."

LangString AbortFileCopy ${LANG_ENGLISH} "Installation was aborted because files could not be copied to $INSTDIR!"
LangString AbortFileCopy ${LANG_GERMAN} "Installation wurde abgebrochen, da keine Dateien in $INSTDIR geschrieben werden konnten!"

LangString StartMenuShortcutErrorMessage ${LANG_ENGLISH} \
"Could not create start menu shortcut! You may have to create one manually.$\n\
Installer is proceeding with installation despite the error."
LangString StartMenuShortcutErrorMessage ${LANG_GERMAN} \
"Konnte keinen Startmenü-Eintrag anlegen! Sie müssen eventuell manuell einen Eintrag anlegen.$\n\
Die Installation wird trotz des Fehlers fortgesetzt."

LangString DesktopShortcutErrorMessage ${LANG_ENGLISH} \
"Could not create desktop shortcut! You may have to create one manually.$\n\
Installer is proceeding with installation despite the error."
LangString DesktopShortcutErrorMessage ${LANG_GERMAN} \
"Konnte keine Desktop-Verknüpfung anlegen! Sie müssen eventuell manuell ein Verknüpfung anlegen.$\n\
Die Installation wird trotz des Fehlers fortgesetzt."



# Uninstaller Messages
LangString OOoKillFailedMessage ${LANG_ENGLISH} \
"LibreOffice/OpenOffice could not be closed. This could have several reasons:$\n\
- No Java found$\n\
- No LibreOffice/OpenOffice found (maybe it isn't installed)$\n\
- Something is keeping an LibreOffice/OpenOffice process alive$\n\
If you are sure that no LibreOffice/OpenOffice process is running you can safely continue the uninstallation.$\n\
Do you want to continue?"
LangString OOoKillFailedMessage ${LANG_GERMAN} \
"LibreOffice/OpenOffice konnte nicht beendet werden. Dies kann verschiedene Gründe haben:$\n\
- Kein Java gefunden$\n\
- Kein LibreOffice/OpenOffice gefunden (evtl. ist es nicht installiert)$\n\
- Irgendein Programm hält einen LibreOffice/OpenOffice-Prozess am Leben$\n\
Wenn Sie sicher sind, dass kein LibreOffice/OpenOffice-Prozess mehr läuft, können Sie die Deinstallation einfach fortsetzen.$\n\
Möchten Sie fortsetzen?"

LangString unNoOOoFoundMessage ${LANG_ENGLISH} \
"Could not uninstall ${WOLLMUX}.oxt because no LibreOffice/OpenOffice installation was found!$\n\
Please uninstall ${WOLLMUX}.oxt manually using the LibreOffice/OpenOffice extension manager."
LangString unNoOOoFoundMessage ${LANG_GERMAN} \
"Konnte ${WOLLMUX}.oxt nicht deinstallieren, da keine LibreOffice/OpenOffice-Installation gefunden wurde!$\n\
Bitte deinstallieren Sie die ${WOLLMUX}.oxt manuell mit dem Extension Manager von LibreOffice/OpenOffice."

LangString InstDirNotDeletedMessage ${LANG_ENGLISH} "The directory $INSTDIR was not deleted since it still contains files or deletion was prevented by the operating system!"
LangString InstDirNotDeletedMessage ${LANG_GERMAN} "Der Ordner $INSTDIR wurde nicht gelöscht, da er noch Dateien enthält oder das Löschen vom Betriebssystem verhindert wurde!"
