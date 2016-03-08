# Die Ausführung von Skripten muss auf dem System erlaubt werden...
# Weitere Informationen finden Sie unter "about_Execution_Policies" (http://go.microsoft.com/fwlink/?LinkID=135170).
# Set-ExecutionPolicy RemoteSigned -Scope CurrentUser

# Parameter
[CmdletBinding()]
Param(
  [Parameter(Mandatory=$True)]
   [string]$versionFile,

   [Parameter(Mandatory=$True)]
   [string]$buildinfoFile,

   [Parameter(Mandatory=$True)]
   [string]$descriptionXmlFile,

   [Parameter(Mandatory=$True)]
   [string]$updateXmlFile
)

if(Test-Path $versionFile) {
    $content = Get-Content $versionFile

    foreach ($versionFileContent in $content)
    {
        $version = $versionFileContent
    }
}

$version | Out-File -FilePath $versionFile -encoding ASCII
echo "Version: ${version}" | Out-File -FilePath $buildinfoFile -encoding ASCII

$descriptionXml = '<?xml version="1.0" encoding="UTF-8"?>
<description xmlns="http://openoffice.org/extensions/description/2006"
	     xmlns:xlink="http://www.w3.org/1999/xlink">
	<version value="' + ${version} + '" />
	<identifier value="de.muenchen.allg.d101.wollmux"/>
	<dependencies>
		<OpenOffice.org-minimal-version value="2.1" name="OpenOffice.org 2.1"/>
	</dependencies>

	<publisher>
		<name xlink:href="http://www.wollmux.org" lang="de">WollMux.org</name>
	</publisher>

	<display-name>
		<name lang="de-DE">WollMux</name>
	</display-name>

	<update-information>
	  <src xlink:href="http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/packages/WollMux-snapshot/WollMux.update.xml"/>
	</update-information>
<!--	<registration>
		<simple-license accept-by="admin" default-license-id="en-NZ" suppress-on-update="true" >
			<license-text xlink:href="registration/license_de-DE.txt" lang="de-DE" />
			<license-text xlink:href="registration/license_en-GB.txt" lang="en-GB" />
			<license-text xlink:href="registration/license_en-NZ.txt" lang="en-NZ" license-id="en-NZ" />
			<license-text xlink:href="registration/license_en-US.txt" lang="en-US" />
		</simple-license>
	</registration>
-->
</description>
' | Out-File -FilePath $descriptionXmlFile -encoding ASCII

$updateXml = '<?xml version="1.0" encoding="UTF-8"?>
<description xmlns="http://openoffice.org/extensions/update/2006"
	     xmlns:xlink="http://www.w3.org/1999/xlink">
	<version value="' + ${version} + '" />
	<identifier value="de.muenchen.allg.d101.wollmux"/>
	<update-download>
          <src xlink:href="http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/packages/WollMux-snapshot/WollMux.oxt"/>
	</update-download>
</description>
' | Out-File -FilePath $updateXmlFile -encoding ASCII

