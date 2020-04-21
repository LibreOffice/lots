package de.muenchen.allg.itd51.wollmux.slv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.util.L;

public class PrintBlockSignatureTest
{

  @Test
  public void testPrintBlockSignature()
  {
    PrintBlockSignature signature = PrintBlockSignature.valueOfIgnoreCase("allversions");
    assertEquals("AllVersions", signature.getName());
    assertEquals("SLV_AllVersions", signature.getGroupName());
    assertEquals(L.m("wird immer gedruckt"), signature.getMessage());
    assertThrows(IllegalArgumentException.class, () -> PrintBlockSignature.valueOfIgnoreCase("foo"));
  }
}
