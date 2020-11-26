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
package de.muenchen.allg.itd51.wollmux.mailmerge.ifthenelse;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;

/**
 * The model of an "IF" node.
 */
public class IfModel extends IfThenElseBaseModel
{
  private String field;
  private boolean not;
  private TestType comparator;
  private IfThenElseBaseModel thenModel;
  private IfThenElseBaseModel elseModel;

  /**
   * Create the model from scratch.
   *
   * @param thenModel
   *          The "THEN" node.
   * @param elseModel
   *          The "ELSE" node.
   */
  public IfModel(IfThenElseBaseModel thenModel, IfThenElseBaseModel elseModel)
  {
    setThenModel(thenModel);
    setElseModel(elseModel);
    field = "";
    not = false;
    setComparator(TestType.STRCMP);
  }

  /**
   * Create the model from a configuration.
   *
   * @param conf
   *          The configuration.
   */
  public IfModel(ConfigThingy conf)
  {
    try
    {
      if (isIfConfig(conf))
      {
        Iterator<ConfigThingy> iter = conf.iterator();
        ConfigThingy ifConf = iter.next();
        ConfigThingy thenConf = iter.next();
        ConfigThingy elseConf = iter.next();

        parsetThen(thenConf);
        parseElse(elseConf);

        if (ifConf.getName().equals("NOT"))
        {
          ifConf = ifConf.getFirstChild();
          setNot(true);
        }

        String comparatorStr = ifConf.getName();
        Optional<TestType> resultTestType = Arrays.stream(TestType.values())
            .filter(item -> comparatorStr.equals(item.getFunc())).findFirst();
        if (ifConf.count() != 2 || !resultTestType.isPresent())
        {
          throw new NodeNotFoundException();
        }
        setComparator(resultTestType.get());

        ConfigThingy value = ifConf.getFirstChild();
        if (value.getName().equals(VALUE_FUNCTION) && value.count() == 1 && value.getFirstChild().count() == 0)
        {
          setValue(ifConf.getLastChild().toString());
          setField(value.toString());
        }
      } else
      {
        throw new NodeNotFoundException();
      }
    } catch (NodeNotFoundException e)
    {
      field = "";
      not = false;
      comparator = TestType.STRCMP;
      setThenModel(new ThenModel());
      setElseModel(new ElseModel());
    }
  }

  private boolean isIfConfig(ConfigThingy conf)
  {
    return conf.getName().equals("IF") && conf.count() == 3;
  }

  private void parsetThen(ConfigThingy thenConf) throws NodeNotFoundException
  {
    if (thenConf.getName().equals("THEN"))
    {
      if (isIfConfig(thenConf.getFirstChild()))
      {
        setThenModel(new IfModel(thenConf.getFirstChild()));
      } else
      {
        setThenModel(new ThenModel(thenConf));
      }
    } else
    {
      setThenModel(new ThenModel());
    }
  }

  private void parseElse(ConfigThingy elseConf) throws NodeNotFoundException
  {
    if (elseConf.getName().equals("ELSE"))
    {
      if (isIfConfig(elseConf.getFirstChild()))
      {
        setElseModel(new IfModel(elseConf.getFirstChild()));
      } else
      {
        setElseModel(new ElseModel(elseConf));
      }
    } else
    {
      setElseModel(new ElseModel());
    }
  }

  public IfThenElseBaseModel getThenModel()
  {
    return thenModel;
  }

  public void setThenModel(IfThenElseBaseModel thenModel)
  {
    this.thenModel = thenModel;
    thenModel.setParent(this);
  }

  public IfThenElseBaseModel getElseModel()
  {
    return elseModel;
  }

  public void setElseModel(IfThenElseBaseModel elseModel)
  {
    this.elseModel = elseModel;
    elseModel.setParent(this);
  }

  public String getField()
  {
    return field;
  }

  public void setField(String field)
  {
    this.field = field;
  }

  public boolean isNot()
  {
    return not;
  }

  public void setNot(boolean not)
  {
    this.not = not;
  }

  public TestType getComparator()
  {
    return comparator;
  }

  public void setComparator(TestType comparator)
  {
    this.comparator = comparator;
    if (this.comparator == null)
    {
      this.comparator = TestType.STRCMP;
    }
  }

  /**
   * Get the node from the nested {@link IfModel#getThenModel()} and {@link IfModel#getElseModel()}.
   */
  @Override
  public IfThenElseBaseModel getById(String id)
  {
    IfThenElseBaseModel model = super.getById(id);
    if (model != null)
    {
      return model;
    }
    model = thenModel.getById(id);
    if (model != null)
    {
      return model;
    }
    return elseModel.getById(id);
  }

  @Override
  public String toString()
  {
    return "WENN " + field + " " + (not ? "nicht " : "") + comparator.getLabel() + " " + getValue();
  }

  /**
   * Create the configuration but create an "IF" function instead.
   */
  @Override
  public ConfigThingy create()
  {
    ConfigThingy conf = new ConfigThingy("IF");

    ConfigThingy func = new ConfigThingy(comparator.getFunc());
    func.add(IfThenElseBaseModel.VALUE_FUNCTION).add(field == null ? "" : field);
    func.add(getValue() == null ? "" : getValue());
    if (not)
    {
      ConfigThingy notConf = new ConfigThingy("NOT");
      notConf.addChild(func);
      func = notConf;
    }
    conf.addChild(func);

    ConfigThingy thenConf = new ConfigThingy("THEN");
    thenConf.addChild(thenModel.create());
    conf.addChild(thenConf);

    ConfigThingy elseConf = new ConfigThingy("ELSE");
    elseConf.addChild(elseModel.create());
    conf.addChild(elseConf);

    return conf;
  }
}
