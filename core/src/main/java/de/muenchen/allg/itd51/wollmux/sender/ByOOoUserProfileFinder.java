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
package de.muenchen.allg.itd51.wollmux.sender;

import de.muenchen.allg.itd51.wollmux.util.L;
import org.libreoffice.ext.unohelper.util.UnoConfiguration;

/**
 * A {@link SenderFinder} using LibreOffice's user data.
 */
class ByOOoUserProfileFinder extends SenderFinder
{
  public ByOOoUserProfileFinder(SenderService senderList)
  {
    super(senderList);
  }

  @Override
  protected String getValueForKey(String key) throws SenderException
  {
    try
    {
      return UnoConfiguration.getConfiguration("/org.openoffice.UserProfile/Data", key).toString();
    } catch (Exception e)
    {
      throw new SenderException(L.m("Value for the key \"{0}\" of OOoUserProfil could not be determined:", key), e);
    }
  }
}
