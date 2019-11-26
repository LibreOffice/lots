/*
 * Dateiname: DJDataset.java
 * Projekt  : WollMux
 * Funktion : Ein vom DJ gelieferter Datensatz, der zu den Methoden von
 *            Dataset noch DJ-spezifische Methoden anbietet.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | BNK | Erstellung
 * 24.10.2005 | BNK | +copy()
 *                  | +remove()
 * 28.10.2005 | BNK | bessere Doku
 *                  | set() wirft jetzt IllegalArgumentException, wenn
 *                  | versucht wird, die Spalte mit null zu overriden
 * -------------------------------------------------------------------
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Ein vom DJ gelieferter Datensatz, der zu den Methoden von Dataset noch
 * DJ-spezifische Methoden anbietet.
 */
public interface DJDataset extends Dataset
{

  /**
   * Schreibt newValue als neuen Wert des Datensatzes in Spalte columnName in den LOS
   * des DJ, jedoch nur falls der Datensatz bereits aus dem LOS kommt (also
   * {@link #isFromLOS()} true liefert). Es ist nicht möglich, mit dieser Funktion
   * einen Spaltenwert als unbelegt (newValue == null) zu überschreiben.
   * 
   * @throws ColumnNotFoundException
   *           falls keine Spalte namens columnName existiert.
   * @throws UnsupportedOperationException
   *           falls dieser Datensatz nicht aus dem LOS kommt.
   * @throws IllegalArgumentException
   *           falls als newValue null übergeben wird.
   */
  public void set(String columnName, String newValue)
      throws ColumnNotFoundException;

  /**
   * Liefert true, falls die Spalte columnName dieses Datensatzes nicht aus den
   * Hintergrunddatenbank kommt, sondern aus dem lokalen Override-Speicher des DJ.
   * Falls der Datensatz gar nicht mit einer Hintergrunddatenbank verknüpft ist
   * (hasBackingStore() == false), so wird hier immer true geliefert, auch wenn der
   * Wert in der Spalte unbelegt ist.
   * 
   * @throws ColumnNotFoundException
   *           falls keine Spalte namens columnName existiert.
   */
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException;

  /**
   * Liefert true, falls zu diesem Datensatz eine Hintergrunddatenbank existiert, mit
   * der einige seiner Spalten verknüpft sind, oder über
   * {@link #discardLocalOverride(String)} verknüpft werden können.
   */
  public boolean hasBackingStore();

  /**
   * Liefert true, falls dieser Datensatz aus dem Lokalen Override Speicher kommt.
   * ACHTUNG! Dies bedeutet nicht, dass es eine Spalte gibt, für die
   * hasLocalOverride(Spalte) true liefert, da der LOS auch Datensätze erlaubt, bei
   * denen alle Spalten noch mit der Hintergrunddatenbank verknüpft sind. Zum
   * Beispiel wird ein Datensatz nicht automatisch aus dem LOS entfernt, wenn für
   * alle Spalten discardLocalOverride() aufgerufen wird.
   */
  public boolean isFromLOS();

  /**
   * Liefert true, falls this der momentan im LOS ausgewählte Datensatz ist.
   */
  public boolean isSelectedDataset();

  /**
   * Macht this zum im LOS ausgewählten Datensatz.
   * 
   * @throws UnsupportedOperationException
   *           falls this nicht aus dem LOS kommt.
   */
  public void select();

  /**
   * Verwirft den Wert im LOS für Spalte columnName dieses Datensatzes und verknüpft
   * die Spalte wieder mit der Hintergrunddatenbank. ACHTUNG! Ein Datensatz bei dem
   * der lokale Override für alle Spalten discardet wurde wird NICHT automatisch aus
   * dem LOS entfernt. Insbesondere liefert isFromLOS() weiterhin true. Die Spalte
   * muss auf jeden Fall existieren.
   * 
   * @throws ColumnNotFoundException
   *           falls keine Spalte namens columnName existiert.
   * @throws NoBackingStoreException
   *           falls der Datensatz nie mit einer Hintergrunddatenbank verknüpft war.
   *           Keine Exception wird geworfen, falls der die entsprechende Spalte
   *           bereits mit einer Hintergrunddatenbank verknüpft ist (z.B. weil der
   *           Datensatz gar nicht aus dem LOS kommt).
   */
  public void discardLocalOverride(String columnName)
      throws ColumnNotFoundException, NoBackingStoreException;

  /**
   * Legt eine Kopie dieses Datensatzes im LOS an. Achtung! Dies verändert nicht den
   * Rückgabewert von this.{@link #isFromLOS()}, da this selbst dadurch nicht
   * verändert wird.
   * 
   * @return die neue Kopie.
   */
  public DJDataset copy();

  /**
   * Entfernt diesen Datensatz aus dem LOS. Achtung! Nach dieser Operation ist der
   * Datensatz ungültig. Insbesondere ist der Wert von {@link #isFromLOS()} nicht
   * definiert.
   * 
   * @throws UnsupportedOperationException
   *           falls dieser Datensatz nicht aus dem LOS kommt.
   */
  public void remove();

}
