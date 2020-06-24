/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
