/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.muenchen.allg.itd51.wollmux.mailmerge.ConnectionModelListener;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;

/**
 * Model of the preview mode of mail merge.
 */
public class PreviewModel implements ConnectionModelListener
{

  private boolean isPreview = false;
  private int previewNumber = 1;
  private Optional<DatasourceModel> model;
  private List<PreviewModelListener> listener = new ArrayList<>();

  /**
   * Create a new model.
   */
  public PreviewModel()
  {
    this.model = Optional.empty();
  }

  /**
   * Adds a listener to the model.
   *
   * @param listener
   *          The listener.
   */
  public void addListener(PreviewModelListener listener)
  {
    this.listener.add(listener);
  }

  public boolean isPreview()
  {
    return isPreview;
  }

  public int getPreviewNumber()
  {
    return previewNumber;
  }

  public void setDatasourceModel(Optional<DatasourceModel> datasourceModel)
  {
    this.model = datasourceModel;
  }

  /**
   * Set preview mode. This calls {@link PreviewModelListener#previewChanged()}.
   *
   * @param isPreview
   *          If true, preview mode is activated in case there's a selected {@link DatasourceModel}
   *          with records.
   * @throws NoTableSelectedException
   *           If no table was selected.
   */
  public void setPreview(boolean isPreview) throws NoTableSelectedException
  {
    model.ifPresentOrElse(ds -> {
      try
      {
        this.isPreview = isPreview && ds.getNumberOfRecords() > 0;
      } catch (NoTableSelectedException ex)
      {
        this.isPreview = false;
      }
    }, () -> this.isPreview = false);
    listener.forEach(PreviewModelListener::previewChanged);
  }

  /**
   * Select a new record Id as preview. Calls {@link PreviewModelListener#previewChanged()} if Id
   * changed.
   *
   * @param previewNumber
   *          The record Id.
   */
  public void setPreviewNumber(int previewNumber)
  {
    int oldPreview = this.previewNumber;
    this.previewNumber = previewNumber;
    if (oldPreview != this.previewNumber)
    {
      listener.forEach(PreviewModelListener::previewChanged);
    }
  }

  /**
   * Set the preview record to the last record of the data source.
   *
   * @throws NoTableSelectedException
   *           If no table is selected.
   */
  public void gotoLastDataset() throws NoTableSelectedException
  {
    if (model.isPresent())
    {
      setPreviewNumber(model.get().getNumberOfRecords());
    }
  }

  /**
   * Get the data belonging to {@link #previewNumber}.
   *
   * @return The data of the record mapping column names to values.
   * @throws NoTableSelectedException
   *           If no table is selected.
   */
  public Map<String, String> getCurrentRecord() throws NoTableSelectedException
  {
    if (model.isPresent())
    {
      return model.get().getRecord(previewNumber);
    } else
    {
      return new HashMap<>();
    }
  }

  @Override
  public void connectionsChanged()
  {
    listener.forEach(PreviewModelListener::previewChanged);
  }

}
