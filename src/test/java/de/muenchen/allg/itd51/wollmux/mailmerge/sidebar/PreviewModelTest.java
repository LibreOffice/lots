package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;

public class PreviewModelTest
{

  @Test
  public void testPreviewModel() throws NoTableSelectedException
  {
    PreviewModel model = new PreviewModel();
    model.setDatasourceModel(Optional.of(new MockDatasourceModel()));
    model.addListener(new PreviewModelListener()
    {
      @Override
      public void previewChanged()
      {
        assertTrue(model.isPreview(), "preview is inactive");
      }
    });
    assertFalse(model.isPreview(), "preview is active");
    model.setPreview(true);

    assertEquals(1, model.getPreviewNumber(), "wrong preview number");
    model.gotoLastDataset();
    assertEquals(5, model.getPreviewNumber(), "wrong preview number");

    assertEquals("5", model.getCurrentRecord().get("Id"), "wrong record");
  }
}
