# This file contains all language specific settings and strings used by the WollMux installer.
# It is included with !include by wollmuxinstaller.nsi.


# Load Language Files
LoadLanguageFile "${NSISDIR}\Contrib\Language files\English.nlf" ;; default language
LoadLanguageFile "${NSISDIR}\Contrib\Language files\German.nlf"

# Section Names
LangString StartMenuShortcut ${LANG_ENGLISH} "Start menu shortcut"
LangString StartMenuShortcut ${LANG_GERMAN} "Startmenü-Eintrag"
LangString DesktopShortcut ${LANG_ENGLISH} "Desktop shortcut"
LangString DesktopShortcut ${LANG_GERMAN} "Desktop-Verknüpfung"

# Start menu link names
LangString UninstallWollMux ${LANG_ENGLISH} "Uninstall WollMux"
LangString UninstallWollMux ${LANG_GERMAN} "WollMux entfernen"

# General Messages
LangString NeedAdminMessage ${LANG_ENGLISH} "You need administrator privileges to execute this program!"
LangString NeedAdminMessage ${LANG_GERMAN} "Sie benötigen Administrator-Rechte um dieses Programm auszuführen!"
LangString TryToKillOOoMessage ${LANG_ENGLISH} "OpenOffice.org must be closed before execution of this program.$\n\
												The program will now check if any OOo windows are open and try to close them."
LangString TryToKillOOoMessage ${LANG_GERMAN} "OpenOffice.org muss vor dem Ausführen dieses Programms geschlossen werden.$\n\
											   Das Programm überprüft nun, ob irgendwelche OOo-Fenster offen sind und versucht diese zu schließen."
LangString OOoRunningMessage ${LANG_ENGLISH} "OpenOffice.org or the OpenOffice.org quickstarter could not be closed!$\n\
											  Please close all OpenOffice.org windows and the quickstarter, then try again."
LangString OOoRunningMessage ${LANG_GERMAN} "OpenOffice.org oder der OpenOffice.org-Schnellstarter konnten nicht beendet werden!$\n\
                                             Bitte schließen Sie alle offenen OpenOffice.org-Fenster sowie den Schnellstarter und versuchen Sie es erneut."
LangString AbortMessage ${LANG_ENGLISH} "Execution aborted!"
LangString AbortMessage ${LANG_GERMAN} "Ausführung abgebrochen!"

# Installer Messages
LangString NoOOoFoundMessage ${LANG_ENGLISH} "Could not install WollMux.oxt because no OpenOffice.org installation was found!$\n\
											  Please install WollMux.oxt manually using the OpenOffice.org extension manager."
LangString NoOOoFoundMessage ${LANG_GERMAN} "Konnte Wollmux.oxt nicht installieren, da keine OpenOffice.org-Installation gefunden wurde!$\n\
											 Bitte installieren Sie die WollMux.oxt manuell mit dem Extension Manager von OpenOffice.org."
LangString UnoPkgErrorMessage ${LANG_ENGLISH} "Error while installing WollMux.oxt extension into OpenOffice.org!$\n\
	                                           Please make sure no process with the name 'soffice.bin' is running and that you have the necessary rights, then try again."
LangString UnoPkgErrorMessage ${LANG_GERMAN} "Fehler beim Installieren der WollMux.oxt-Extension in OpenOffice.org!$\n\
	                                          Bitte stellen Sie sicher, dass kein Prozess mit dem Namen 'soffice.bin' läuft und dass sie die notwendigen Benutzerrechte haben."
LangString AbortUnoPkgErrorMessage ${LANG_ENGLISH} "Installation was aborted because WollMux.oxt extension couldn't be installed!$\n\
	                                                No files were created during installation but an already existing WollMux extension may have been uninstalled."
LangString AbortUnoPkgErrorMessage ${LANG_GERMAN} "Installation wurde abgebrochen, da die WollMux.oxt-Extension nicht installiert werden konnte!$\n\
	                                               Keine Dateien wurden während der Installation erzeugt, aber eventuell wurde eine bereits vorhandene WollMux-Extension deinstalliert."
LangString AbortFileCopy ${LANG_ENGLISH} "Installation was aborted because files could not be copied to $INSTDIR!"
LangString AbortFileCopy ${LANG_GERMAN} "Installation wurde abgebrochen, da keine Dateien in $INSTDIR geschrieben werden konnten!"
LangString StartMenuShortcutErrorMessage ${LANG_ENGLISH} "Could not create start menu shortcut! You may have to create one manually.$\n\
                                                          Installer is proceeding with installation despite the error."
LangString StartMenuShortcutErrorMessage ${LANG_GERMAN} "Konnte keinen Startmenü-Eintrag anlegen! Sie müssen eventuell manuell einen Eintrag anlegen.$\n\
                                                         Die Installation wird trotz des Fehlers fortgesetzt."
LangString DesktopShortcutErrorMessage ${LANG_ENGLISH} "Could not create desktop shortcut! You may have to create one manually.$\n\
                                                        Installer is proceeding with installation despite the error."
LangString DesktopShortcutErrorMessage ${LANG_GERMAN} "Konnte keine Desktop-Verknüpfung anlegen! Sie müssen eventuell manuell ein Verknüpfung anlegen.$\n\
                                                       Die Installation wird trotz des Fehlers fortgesetzt."


# Uninstaller Messages
LangString unNoOOoFoundMessage ${LANG_ENGLISH} "Could not uninstall WollMux.oxt because no OpenOffice.org installation was found!$\n\
											    Please uninstall WollMux.oxt manually using the OpenOffice.org extension manager."
LangString unNoOOoFoundMessage ${LANG_GERMAN} "Konnte Wollmux.oxt nicht deinstallieren, da keine OpenOffice.org-Installation gefunden wurde!$\n\
											   Bitte deinstallieren Sie die WollMux.oxt manuell mit dem Extension Manager von OpenOffice.org."
LangString InstDirNotDeletedMessage ${LANG_ENGLISH} "The directory $INSTDIR was not deleted since it still contains files or deletion was prevented by the operating system!"
LangString InstDirNotDeletedMessage ${LANG_GERMAN} "Der Ordner $INSTDIR wurde nicht gelöscht, da er noch Dateien enthält oder das Löschen vom Betriebssystem verhindert wurde!"
