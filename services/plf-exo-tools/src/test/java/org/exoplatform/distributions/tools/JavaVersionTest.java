/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This file is part of eXo Platform Public Distributions - eXo Tools.
 *
 * eXo Platform Public Distributions - eXo Tools is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * eXo Platform Public Distributions - eXo Tools software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with eXo Platform Public Distributions - eXo Tools; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.distributions.tools;

import org.junit.Test;

import static org.junit.Assert.*;


public class JavaVersionTest {

  @Test
  public final void testBadFormatVersion(){
    JavaVersion jv = new JavaVersion("12_7");

    // if we can't determine the version, we consider that it's Java 8
    assertTrue(jv.isMinorVersionEqual(8) == 0);
    assertTrue(jv.isMinorVersionEqual(9) == -1);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(9) == -1);
  }

  @Test
  public final void testJava8Version(){
    JavaVersion jv = new JavaVersion("1.8.0_25");

    assertTrue(jv.isMinorVersionEqual(8) == 0);
    assertTrue(jv.isMinorVersionEqual(9) == -1);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(9) == -1);

    jv = new JavaVersion("1.8.0");
    assertTrue(jv.isMinorVersionEqual(8) == 0);
    assertTrue(jv.isMinorVersionEqual(9) == -1);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(9) == -1);

    jv = new JavaVersion("1.8");
    assertTrue(jv.isMinorVersionEqual(8) == 0);
    assertTrue(jv.isMinorVersionEqual(9) == -1);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(9) == -1);
  }


  @Test
  public final void testJava9Version(){
    JavaVersion jv = new JavaVersion("9");

    assertTrue(jv.isMinorVersionEqual(8) == -1);
    assertTrue(jv.isMajorVersionEqual(9) == 0);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(9) == 0);

    jv = new JavaVersion("9.0");
    assertTrue(jv.isMinorVersionEqual(8) == -1);
    assertTrue(jv.isMajorVersionEqual(9) == 0);
    assertTrue(jv.isMinorVersionEqual(0) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == -1);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(9) == 0);

    jv = new JavaVersion("9.0.1");
    assertTrue(jv.isMinorVersionEqual(8) == -1);
    assertTrue(jv.isMajorVersionEqual(9) == 0);
    assertTrue(jv.isMinorVersionEqual(0) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == -1);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(9) == 0);
  }

  @Test
  public final void testJava11Version(){
    JavaVersion jv = new JavaVersion("11");

    assertTrue(jv.isMinorVersionEqual(8) == -1);
    assertTrue(jv.isMajorVersionEqual(9) == -1);
    assertTrue(jv.isMajorVersionEqual(11) == 0);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(11) == 0);

    jv = new JavaVersion("11.0");
    assertTrue(jv.isMinorVersionEqual(8) == -1);
    assertTrue(jv.isMajorVersionEqual(9) == -1);
    assertTrue(jv.isMajorVersionEqual(11) == 0);
    assertTrue(jv.isMinorVersionEqual(0) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == -1);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(9) == 0);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(11) == 0);

    jv = new JavaVersion("11.0.1");
    assertTrue(jv.isMinorVersionEqual(8) == -1);
    assertTrue(jv.isMajorVersionEqual(9) == -1);
    assertTrue(jv.isMajorVersionEqual(11) == 0);
    assertTrue(jv.isMinorVersionEqual(0) == 0);
    assertTrue(jv.isMinorVersionSuperiorOrEqual(8) == -1);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(9) == 0);
    assertTrue(jv.isMajorVersionSuperiorOrEqual(11) == 0);
  }

  @Test
  public void testEquals() {
    JavaVersion jv825 = new JavaVersion("1.8.0_25");
    JavaVersion jv845 = new JavaVersion("1.8.0_45");
    JavaVersion jv11 = new JavaVersion("11");
    JavaVersion jv1101 = new JavaVersion("11.0.1");

    assertEquals(jv825, jv825);
    assertEquals(jv825, jv845);
    assertEquals(jv825, jv845);
    assertNotEquals(jv825, new Object());
    assertEquals(jv11, jv1101);
    assertNotEquals(jv825, jv1101);
  }
}
