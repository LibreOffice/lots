/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.gender;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;

/**
 * A model for Gender-TRAFOs.
 */
public class GenderTrafoModel
{
  private static final String GENDER = "Gender";

  private String male = "";
  private String female = "";
  private String other = "";
  private String field = "";
  private String functionName;

  /**
   * A new Gender-TRAFO model.
   *
   * @param conf
   *          The gender function in {@link ConfigThingy} format.
   */
  public GenderTrafoModel(ConfigThingy conf)
  {
    try
    {
      if (conf == null || conf.count() != 1)
      {
        return;
      }
      ConfigThingy bind = conf.getFirstChild();
      if (!bind.getName().equals("BIND"))
      {
        return;
      }

      ConfigThingy funcs = bind.query("FUNCTION", 1);
      if (funcs.count() != 1)
      {
        return;
      }
      ConfigThingy func = funcs.getLastChild();
      if (!func.toString().equals(GenderTrafoModel.GENDER))
      {
        return;
      }

      for (ConfigThingy set : bind)
      {
        parseSet(set);
      }
      functionName = conf.getName();
    } catch (NodeNotFoundException e)
    {
      // not possible as count is tested before the children are accessed.
    }
  }

  public String getMale()
  {
    return male;
  }

  public void setMale(String male)
  {
    this.male = male;
  }

  public String getFemale()
  {
    return female;
  }

  public void setFemale(String female)
  {
    this.female = female;
  }

  public String getOther()
  {
    return other;
  }

  public void setOther(String other)
  {
    this.other = other;
  }

  public String getField()
  {
    return field;
  }

  public void setField(String field)
  {
    this.field = field;
  }

  public String getFunctionName()
  {
    return functionName;
  }

  private void parseSet(ConfigThingy set) throws NodeNotFoundException
  {
    if (!set.getName().equals("SET") || set.count() != 2)
    {
      return;
    }

    String setKey = set.getFirstChild().toString();
    ConfigThingy value = set.getLastChild();

    if (setKey.equals("Anrede") && value.getName().equals("VALUE") && value.count() == 1)
    {
      setField(value.toString());
    } else if (setKey.equals("Falls_Anrede_HerrN") && value.count() == 0)
    {
      setMale(value.toString());
    } else if (setKey.equals("Falls_Anrede_Frau") && value.count() == 0)
    {
      setFemale(value.toString());
    } else if (setKey.equals("Falls_sonstige_Anrede") && value.count() == 0)
    {
      setOther(value.toString());
    }
  }

  /**
   * Generate the gender field description in the form
   * {@code BIND(FUNCTION "Gender" SET("Anrede", VALUE
   * "<anredeFieldId>") SET("Falls_Anrede_HerrN", "<textHerr>") SET("Falls_Anrede_Frau",
   * "<textFrau>") SET("Falls_sonstige_Anrede", "<textSonst>"))}
   *
   * @return A {@link ConfigThingy} describing the gender field.
   */
  public ConfigThingy generateGenderTrafoConf()
  {
    ConfigThingy conf = new ConfigThingy("Func");
    ConfigThingy bind = new ConfigThingy("BIND");
    conf.addChild(bind);
    bind.add("FUNCTION").add(GenderTrafoModel.GENDER);

    ConfigThingy setAnrede = bind.add("SET");
    setAnrede.add("Anrede");
    setAnrede.add("VALUE").add(field);

    ConfigThingy setHerr = bind.add("SET");
    setHerr.add("Falls_Anrede_HerrN");
    setHerr.add(male);

    ConfigThingy setFrau = bind.add("SET");
    setFrau.add("Falls_Anrede_Frau");
    setFrau.add(female);

    ConfigThingy setSonst = bind.add("SET");
    setSonst.add("Falls_sonstige_Anrede");
    setSonst.add(other);

    return conf;
  }
}
