package de.muenchen.allg.itd51.wollmux.core.document;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.core.document.nodes.Container;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.FormControl;


/**
 * Abstrakte Basis-Klasse für das Besuchen aller Knoten eines DocumentTrees. Dazu
 * wird ein Objekt erzeugt, das von Visitor abgeleitet ist und dann auf diesem
 * Objekt obj die Methode obj.visit(tree) aufgerufen, wobei tree ein DocumentTree
 * ist. Für jeden Node des Baums wird dann die entsprechende Methode von Visitor
 * aufgerufen (teilweise mehrfach). Die Methoden können alle false zurückliefern um
 * zu signalisieren, dass das Durchlaufen des Baumes abgebrochen werden soll. Die
 * von Standard-Methoden liefern alle true.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class DocumentTreeVisitor
{
  public void visit(XTextDocument doc)
  {
    DocumentTree tree = new DocumentTree(doc);
    tree.getRoot().visit(this);
  }

  /**
   * Wird für Knoten aufgerufen, die eine Einfügestelle (insertValue,
   * insertFormValue) repräsentieren.
   * 
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean insertionBookmark(InsertionBookmark bookmark)
  {
    return true;
  }

  /**
   * Wird für Knoten aufgerufen, die Formularsteuerelement repräsentieren.
   * 
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean formControl(FormControl control)
  {
    return true;
  }

  /**
   * Wird für Knoten aufgerufen, die im inneren des Baumes liegen, d,h, Kindknoten
   * haben können. Dies sind zum Beispiel Absätze.
   * 
   * @param count
   *          Der Knoten wird einmal mit count == 0 besucht bevor der erste
   *          Nachfahre besucht wird und einmal nach dem Besuchen aller Nachfahren
   *          mit count == 1.
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean container(Container container, int count)
  {
    return true;
  }

  /**
   * Wird für Knoten aufgerufen, die Textabschnitte repräsentieren.
   * 
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean textRange(TextRange textRange)
  {
    return true;
  }
}