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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Extract Java Minor version as int from a String Java version in official format.
 *
 * <p>
 *   Example of String Java Version:
 *   <ul>
 *     <li>Java 8: 1.8, 1.8.0, 1.8.0_65</li>
 *     <li>Java 9: 9, 9.0, 9.1.1</li>
 *   </ul>
 * </p>
 *
 */
class JavaVersion {

  /** Pattern based on official Java version format */
  final Pattern regex = Pattern.compile("([\\d]+)(?:[\\.\\+]([\\d]+)(?:.*|$))?");

  /** Default value for Java major version */
  private int systemJavaMajorVersion = 1;

  /** Default value for Java minor version */
  private int systemJavaMinorVersion = 8;

  JavaVersion(String version) {
    extractVersions(version);
  }

  /**
   * Extract major and minor versions as int from a String Java version.
   *
   * @param systemJavaVersion Java Version as String ie: 1.8.0_45
   */
  void extractVersions(final String systemJavaVersion) {
    try {
      Matcher regexMatcher = regex.matcher(systemJavaVersion);
      if (regexMatcher.find()) {
        systemJavaMajorVersion = Integer.parseInt(regexMatcher.group(1));
        String minorVersion = regexMatcher.group(2);
        if(minorVersion != null) {
          systemJavaMinorVersion = Integer.parseInt(minorVersion);
        } else if(systemJavaMajorVersion == 9) {
          systemJavaMinorVersion = 0;
        }
      }
    } catch (Exception ex){
      // By default JDK 8 version is used.
    }
  }

  /**
   * Check if the version is superior or equal to the Java System Major Version.
   * @param version Java major version to compare
   * @return 0 if true, -1 otherwise
   */
  public final int isMajorVersionSuperiorOrEqual(int version) {
    return ((systemJavaMajorVersion >= version) ? 0 : -1);
  }

  /**
   * Check if the version is equal to the Java System Major Version.
   * @param version Java major version to compare
   * @return 0 if true, -1 otherwise
   */
  public final int isMajorVersionEqual(int version) {
    return ((systemJavaMajorVersion == version) ? 0 : -1);
  }


  /**
   * Check if the version is superior or equal to the Java System Minor Version.
   * @param version Java minor version to compare
   * @return 0 if true, -1 otherwise
   */
  public final int isMinorVersionSuperiorOrEqual(int version) {
    return ((systemJavaMinorVersion >= version) ? 0 : -1);
  }

  /**
   * Check if the version is equal to the Java System Minor Version.
   * @param version Java minor version to compare
   * @return 0 if true, -1 otherwise
   */
  public final int isMinorVersionEqual(int version) {
    return ((systemJavaMinorVersion == version) ? 0 : -1);
  }

  public int getSystemJavaMajorVersion() {
    return systemJavaMajorVersion;
  }

  public int getSystemJavaMinorVersion() {
    return systemJavaMinorVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    JavaVersion that = (JavaVersion) o;
    return Objects.equals(systemJavaMajorVersion, that.systemJavaMajorVersion) &&
            Objects.equals(systemJavaMinorVersion, that.systemJavaMinorVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(systemJavaMajorVersion, systemJavaMinorVersion);
  }

}
