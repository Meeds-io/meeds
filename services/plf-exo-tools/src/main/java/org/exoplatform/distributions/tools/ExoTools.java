/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.distributions.tools;

/**
 * Main class for the exo-tools JAR.<br>
 *
 * This library is used by the startup scripts to configure the environment (JVM)
 * according to system parameters.
 *
 * The exo-tools.jar is present at the following path:
 * <ul>
 *   <li>PLF Tomcat: $CATALINA_HOME/bin/exo-tools.jar</li>
 *   <li>PLF JBoss: $JBOSS_HOME/bin/exo-tools.jar</li>
 * </ul>
 */
public final class ExoTools {

  /** Java Version from the system */
  public static final String systemJavaVersion   = System.getProperty("java.version");

  public static final String CMD_JAVA_8          = "isJava8";

  public static final String CMD_JAVA_8_OR_MORE  = "isJava8OrSuperior";

  public static final String CMD_JAVA_9          = "isJava9";

  public static final String CMD_JAVA_9_OR_MORE  = "isJava9OrSuperior";

  public static final String CMD_JAVA_11         = "isJava11";

  public static final String CMD_JAVA_11_OR_MORE = "isJava11OrSuperior";

  /**
   * eXo Tools entrypoint.
   *
   * @param args the command to execute
   */
  public static void main(String args[]) {
    int result = doMain(args);
    System.exit(result);
  }

  /**
   * Check the prerequisites and execute the process
   * according to the given command.
   *
   * @param args the command to execute
   * @return 0 if the process is OK, -1 otherwise
   */
  private static int doMain(String args[]) {

    if (null == systemJavaVersion || systemJavaVersion.equals("") || args.length == 0) {
      return -1;
    }
    final JavaVersion javaVersion = new JavaVersion(systemJavaVersion);

    switch (args[0]) {
      case CMD_JAVA_8:
        return javaVersion.isMinorVersionEqual(8);
      case CMD_JAVA_8_OR_MORE:
        return javaVersion.isMinorVersionSuperiorOrEqual(8);
      case CMD_JAVA_9:
        return javaVersion.isMajorVersionEqual(9);
      case CMD_JAVA_9_OR_MORE:
        return javaVersion.isMajorVersionSuperiorOrEqual(9);
      case CMD_JAVA_11:
        return javaVersion.isMajorVersionEqual(11);
      case CMD_JAVA_11_OR_MORE:
        return javaVersion.isMajorVersionSuperiorOrEqual(11);
      default:
        return -1;
    }
  }
}
