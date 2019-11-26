package de.muenchen.allg.itd51.wollmux.core.document;

/**
 * Wird geworfen, wenn das verwendete OpenOffice.org das RDF-Metadaten-Interface
 * noch nicht unterstützt.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class RDFMetadataNotSupportedException extends Exception
{
  private static final long serialVersionUID = 2416952716636541797L;

  public RDFMetadataNotSupportedException()
  {
    super();
  }

  public RDFMetadataNotSupportedException(Throwable cause)
  {
    super(cause);
  }
}