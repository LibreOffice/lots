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
package de.muenchen.allg.itd51.wollmux.mailmerge.ifthenelse;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;

/**
 * The model for the If-Then-Else-Dialog.
 */
public class IfThenElseModel
{
  private IfModel function;
  private String name;

  /**
   * Create a new model form a configuration.
   *
   * @param conf
   *          The configuration.
   */
  public IfThenElseModel(ConfigThingy conf)
  {
    if (conf == null || conf.count() != 1)
    {
      function = new IfModel(new ThenModel(), new ElseModel());
    } else
    {
      try
      {
        function = new IfModel(conf.getFirstChild());
        name = conf.getName();
      } catch (NodeNotFoundException e)
      {
        // Can not happen as we tested count before
        function = new IfModel(new ThenModel(), new ElseModel());
      }
    }
  }

  public IfModel getFunction()
  {
    return function;
  }

  public String getName()
  {
    return name;
  }

  /**
   * Replace the node with the given ID with a new condition node.
   *
   * @param id
   *          The ID.
   * @return The new node.
   */
  public IfModel createNewCondition(String id)
  {
    IfThenElseBaseModel model = getById(id);
    if (model != null)
    {
      model = model.getParent();
      if (model instanceof IfModel)
      {
        IfModel newModel = new IfModel(new ThenModel(), new ElseModel());
        IfModel parentModel = (IfModel) model;
        if (parentModel.getThenModel().getId().equals(id))
        {
          parentModel.setThenModel(newModel);
          return newModel;
        } else
        {
          parentModel.setElseModel(newModel);
          return newModel;
        }
      }
    }
    return null;
  }

  /**
   * Delete the condition with the given ID or where its children has the ID. Replace it with either
   * a {@link ThenModel} or {@link ElseModel}. The root condition can't be deleted. R
   *
   * @param id
   *          The ID.
   * @return The new node.
   */
  public IfThenElseBaseModel deleteCondition(String id)
  {
    IfThenElseBaseModel deleteModel = getById(id);
    while (deleteModel != null && !(deleteModel instanceof IfModel))
    {
      deleteModel = deleteModel.getParent();
    }

    if (deleteModel == null || deleteModel.getParent() == null)
    {
      return null;
    }

    IfModel parent = (IfModel) deleteModel.getParent();
    if (parent.getThenModel().getId().equals(deleteModel.getId()))
    {
      parent.setThenModel(new ThenModel());
      return parent.getThenModel();
    } else
    {
      parent.setElseModel(new ElseModel());
      return parent.getElseModel();
    }
  }

  /**
   * Find a node with the given ID.
   *
   * @param id
   *          The ID.
   * @return The node or null if no such not can be found.
   */
  public IfThenElseBaseModel getById(String id)
  {
    return function.getById(id);
  }

  /**
   * Create a configuration from the model.
   *
   * @return The configuration.
   */
  public ConfigThingy create()
  {
    ConfigThingy currentConfig = new ConfigThingy("Func");
    currentConfig.addChild(function.create());
    return currentConfig;
  }
}
