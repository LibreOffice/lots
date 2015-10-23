@echo off

"${oooPath}\unopkg" remove WollMux.uno.pkg
"${oooPath}\unopkg" remove WollMux.uno.pkg --shared
"${oooPath}\unopkg" remove de.muenchen.allg.d101.wollmux
"${oooPath}\unopkg" remove de.muenchen.allg.d101.wollmux --shared
exit 0
