/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.mailmerge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;

import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Description of a replacement of a form field by new fields or text. A description can be combined
 * of new fields and text. This are the rules for substitution:
 * <ol>
 * <li><b>Substitution with exactly one field</b>: The id in insertFormValue-commands and mail merge
 * fields is replaced by {@link #newFieldId}. The Trafo of a user field is modified so that the
 * value-functions use {@link #newFieldId}.</li>
 *
 * <li><b>Substitution with more than one field</b>: This substitution can't be applied on
 * transformed fields. InsertFormValue-commands and mail merge fields are replaced by several new
 * commands/fields and some fixed text if necessary.</li>
 *
 * <li><b>Empty substitution</b>: Nothing is done</li>
 * </ol>
 */
public class FieldSubstitution implements Iterable<FieldSubstitution.SubstElement>
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FieldSubstitution.class);

  /**
   * List of all substitution elements.
   */
  private List<FieldSubstitution.SubstElement> elements = new ArrayList<>();

  /**
   * If it's a 1-to-1 substitution, this field contains the new id. Otherwise its null;
   */
  private String newFieldId = null;

  /**
   * Add a substitution by a field.
   *
   * @param fieldname
   *          The name of the field.
   */
  public void addField(String fieldname)
  {
    if (elements.isEmpty())
    {
      newFieldId = fieldname;
    } else
    {
      /*
       * we have a substitution with more fields, so the new name is unambiguous.
       */
      newFieldId = null;
    }
    elements.add(new SubstElement(SubstElementType.FIELD, fieldname));
  }

  /**
   * Add a substitution by fixed text.
   *
   * @param text
   *          The text.
   */
  public void addFixedText(String text)
  {
    elements.add(new SubstElement(SubstElementType.FIXED_TEXT, text));
    newFieldId = null;
  }

  @Override
  public Iterator<FieldSubstitution.SubstElement> iterator()
  {
    return elements.iterator();
  }

  @Override
  public String toString()
  {
    StringBuilder buffy = new StringBuilder();
    for (FieldSubstitution.SubstElement ele : this)
    {
      buffy.append(ele.isField() ? "<" + ele.getValue() + ">" : ele.getValue());
    }
    return buffy.toString();
  }

  /**
   * Apply this substitution on all occurrences of fieldId in the document. Updates also the form
   * description.
   *
   * @param controller
   *          The controller of the document in which the field is to be replaced.
   * @param fieldId
   *          The id of the field which should be replaced.
   */
  public void apply(TextDocumentController controller, String fieldId)
  {
    if (elements.isEmpty())
    {
      return;
    }

    List<FormField> c = controller.getModel().getIdToFormFields().get(fieldId);
    if (c != null)
    {
      for (FormField f : c)
      {
        if (f.getTrafoName() != null)
        {
          updateTrafoField(controller, f, true);
        } else
        {
          updateField(controller, f, true);
        }
      }
    }

    c = controller.getModel().getIdToTextFieldFormFields().get(fieldId);
    if (c != null)
    {
      for (FormField f : c)
      {
        if (f.getTrafoName() != null)
        {
          updateTrafoField(controller, f, false);
        } else
        {
          updateField(controller, f, false);
        }
      }
    }
  }

  /**
   * Update a field.
   *
   * @param controller
   *          The controller of the document which contains the field.
   * @param formField
   *          The field.
   * @param isInsertFormValue
   *          Is the form field to be modified an InsertFormValue field.
   */
  private void updateField(TextDocumentController controller, FormField formField,
      boolean isInsertFormValue)
  {
    try
    {
      XTextRange anchor = formField.getAnchor();
      if (formField.getAnchor() != null)
      {
        // create cursor, delete field and set text.
        XTextCursor cursor = anchor.getText().createTextCursorByRange(anchor);
        formField.dispose();
        cursor.setString(toString());

        // replace fields with book marks
        cursor.collapseToStart();
        for (FieldSubstitution.SubstElement ele : elements)
        {
          if (ele.isFixedText())
          {
            cursor.goRight((short) ele.getValue().length(), false);
          } else if (ele.isField())
          {
            cursor.goRight((short) (1 + ele.getValue().length() + 1), true);
            if (isInsertFormValue)
            {
              new Bookmark("WM(CMD 'insertFormValue' ID '" + ele.getValue() + "')", controller.getModel().doc, cursor);
            } else
            {
              controller.insertMailMergeField(ele.getValue(), cursor);
            }
            cursor.collapseToEnd();
          }
        }
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }

  /**
   * Update a field with Trafo.
   *
   * @param controller
   *          The controller of the document which contains the field.
   * @param formField
   *          The field.
   * @param isInsertFormValue
   *          Is the form field to be modified an InsertFormValue field.
   */
  private void updateTrafoField(TextDocumentController controller, FormField formField,
      boolean isInsertFormValue)
  {
    if (newFieldId != null)
    {
      if (isInsertFormValue)
      {
        formField.substituteFieldID(formField.getId(), newFieldId);
      } else
      {
        substituteFieldIdInTrafo(controller, formField.getTrafoName(), formField.getId(),
            newFieldId);
      }
    } else
    {
      LOGGER.error("Kann transformiertes Feld nur durch eine 1-zu-1 Zuordnung ersetzen.");
    }
  }

  /**
   * Replace all occurrences of oldFieldId in a Trafo and stores the function. If any of the
   * parameters is null nothing is done.
   *
   * @param controller
   *          The controller of the document.
   * @param trafoName
   *          The name of the Trafo.
   * @param oldFieldId
   *          The old field id.
   * @param newFieldId
   *          The new field id.
   */
  private void substituteFieldIdInTrafo(TextDocumentController controller, String trafoName,
      String oldFieldId, String newFieldId)
  {
    if (trafoName == null || oldFieldId == null || newFieldId == null)
    {
      return;
    }
    try
    {
      ConfigThingy trafoConf = controller.getModel().getFormDescription().query("Formular")
          .query("Funktionen").query(trafoName, 2).getLastChild();
      substituteValueRecursive(trafoConf, oldFieldId, newFieldId);

      // persist form description
      controller.storeCurrentFormDescription();

      // parse function and update library
      FunctionLibrary funcLib = controller.getFunctionLibrary();
      Function func = FunctionFactory.parseChildren(trafoConf, funcLib,
          controller.getDialogLibrary(), controller.getFunctionContext());
      funcLib.add(trafoName, func);
    } catch (NodeNotFoundException e)
    {
      LOGGER.error(
          "Die trafo '{}' ist nicht in diesem Dokument definiert und kann daher nicht verändert werden.",
          trafoName);
    } catch (ConfigurationErrorException e)
    {
      // Shouldn't happen because old Trafo was valid.
      LOGGER.error("", e);
    }
  }

  /**
   * Substitute all VALUE nodes in the configuration which have exactly one child with oldFieldId.
   *
   * @param conf
   *          The configuration.
   * @param oldFieldId
   *          The old id, which have to be substituted.
   * @param newFieldId
   *          THe new id.
   */
  private static void substituteValueRecursive(ConfigThingy conf, String oldFieldId,
      String newFieldId)
  {
    if (conf == null)
    {
      return;
    }

    if ("VALUE".equals(conf.getName()) && conf.count() == 1 && conf.toString().equals(oldFieldId))
    {
      try
      {
        conf.getLastChild().setName(newFieldId);
      } catch (NodeNotFoundException e)
      {
        // we tested, that there's a child.
      }
      return;
    }

    for (ConfigThingy child : conf)
    {
      substituteValueRecursive(child, oldFieldId, newFieldId);
    }
  }

  /**
   * Types of substitution elements.
   */
  public enum SubstElementType
  {
    /**
     * Some fixed text substitution.
     */
    FIXED_TEXT,
    /**
     * A field substitution.
     */
    FIELD;
  }

  /**
   * Part of a substitution.
   */
  public class SubstElement
  {
    private SubstElementType type;

    private String value;

    /**
     * Create this substitution element.
     *
     * @param type
     *          The type of substitution.
     * @param value
     *          The value of the substitution.
     */
    public SubstElement(SubstElementType type, String value)
    {
      this.value = value;
      this.type = type;
    }

    /**
     * Get the value.
     *
     * @return If {@link #isField()} returns true, this is an Id of a field. If
     *         {@link #isFixedText()} returns true, this is some fixed text.
     */
    public String getValue()
    {
      return value;
    }

    /**
     * Is this a field?
     *
     * @return True if its type is {@link SubstElementType#FIELD}.
     */
    public boolean isField()
    {
      return type == SubstElementType.FIELD;
    }

    /**
     * Is this a field?
     *
     * @return True if its type is {@link SubstElementType#FIXED_TEXT}.
     */
    public boolean isFixedText()
    {
      return type == SubstElementType.FIXED_TEXT;
    }

    @Override
    public String toString()
    {
      return (isField() ? "FIELD" : "FIXED_TEXT") + " \"" + value + "\"";
    }
  }
}
