#!/usr/bin/python3

import html5lib
import xml.etree.ElementTree as ET
from markdownify import markdownify as md
from textwrap import wrap
import argparse


def create_changelog(filename, version):
    """
    Liest aus einem HTML-File einen Roadmap Eintrag für den WollMux und fügt ihn dem Changelog hinzu.
    :param filename: Die Datei aus der der Roadmap Eintrag gelesen werden soll.
    :param version: Die Version des Roadmap Eintrags.
    :return: nichts
    """
    with open(filename, "r") as f:
        document = html5lib.parse(f, treebuilder="lxml")
    releases = document.xpath(".//*[@id='Version_" + version + "']/..")
    with open("ChangeLog", "r") as cf:
        orig = cf.read()
    with open("ChangeLog", "w") as cf:
        changelog = []
        for release in releases:
            changelog.extend(parse_release(release))
        cf.writelines(changelog)
        cf.write(orig)


def parse_release(release):
    """
    Extrahiert die notwendigen Informationen aus dem Roadmap Eintrag und baut den Changelog Eintrag zusammen.
    :param release: Das HTML-Tag mit der Überschrift.
    :return: Der Changelog Eintrag.
    """
    version = release.xpath(".//*[starts-with(@id, 'Version_')]")[0]

    sib = release.getnext()
    notes = []
    while True and not sib is None:
        n = sib.xpath(".//*[starts-with(@id, 'Release_Notes')]/..")
        if len(n) != 0:
            notes.append(n[0].getnext())
        if len(sib.xpath(".//*[starts-with(@id, 'Version_')]")) != 0:
            break
        sib = sib.getnext()
    return make_entry(version, notes)


def make_entry(version, notes):
    """
    Erstellt aus der Version und den Release Notes einen Changelog Eintrag.
    :param version: Die Version des Eintrags.
    :param notes: Eine Liste mit Release Notes.
    :return: Der Changelog Eintrag.
    """
    entry = [" " + "=" *20 + " Neu in " + version.text + " " + "=" * 20 + "\n\n"]
    for note in notes:
        text = ET.tostring(note, encoding="unicode", method="html")
        text = text.replace("html:", "")
        lines = md(text, convert=['li', 'ol', 'ul'], bullets='*o¤').splitlines()
        for line in lines:
            line = line.rstrip()
            if len(line) == 0:
                continue
            line = line.replace("¤", "#")
            line = line.replace("\t ", " "*6)
            line = line.replace("\t", " "*6)
            count = len(line)-len(line.lstrip()) + 4
            if count == 5:
                count = 4
            wrapped_lines = wrap(line.lstrip(), width=80, initial_indent=" "*count, subsequent_indent=" "*(count + 2))
            for wl in wrapped_lines:
                entry.append(wl + "\n")
    entry.append("\n")
    return entry


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Lädt einen Roadmap Eintrag herunter und formatiert ihn, so dass er ins Changelog eingefügt werden kann.")
    parser.add_argument('filename', metavar='filename', help="HTML-Datei die die Roadmap enthält.")
    parser.add_argument('version', metavar='version', help="Version des Roadmap Eintrags, der dem Changelog angefügt werden soll.")

    args = parser.parse_args()
    create_changelog(args.filename, args.version)
