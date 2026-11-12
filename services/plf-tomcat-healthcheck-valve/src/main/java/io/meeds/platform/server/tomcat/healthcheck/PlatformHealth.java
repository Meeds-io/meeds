/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2026 Meeds Association
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
package io.meeds.platform.server.tomcat.healthcheck;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;

/**
 * Reads the state of the platform, without any session nor request to a web
 * application. The platform is healthy when every web application of the
 * host is started and the portal container is started.
 */
final class PlatformHealth {

  /** Value that disables the check of the portal container. */
  private static final String  NONE = "none";

  /** Characters allowed in the name of the portal container. */
  private static final Pattern CONTAINER_NAME = Pattern.compile("[A-Za-z0-9_.-]+");

  /** Name of the portal container to check, empty to skip it. */
  private final String         portalContainer;

  /**
   * @param containerName name of the portal container that must be started,
   *                      empty or "none" to skip the portal container check
   */
  PlatformHealth(String containerName) {
    String name = containerName.trim();
    this.portalContainer = NONE.equalsIgnoreCase(name) ? "" : name;
  }

  /**
   * @param host the host of the web applications
   * @return the reasons why the platform is not healthy, empty when it is
   */
  List<String> problems(Host host) {
    List<String> problems = new ArrayList<>();
    for (Container child : host.findChildren()) {
      if (child instanceof Context context && context.getState() != LifecycleState.STARTED) {
        // The name of the ROOT web application is empty
        String name = context.getName().isEmpty() ? "/" : context.getName();
        problems.add("webapp '" + name + "' is " + context.getState());
      }
    }
    if (!portalContainer.isEmpty()) {
      checkPortalContainer(problems);
    }
    return problems;
  }

  /**
   * The container exposes its <code>Started</code> attribute in JMX, which is
   * read without loading any class of the platform nor creating anything.
   *
   * @param problems receives the reason when the container is not started
   */
  private void checkPortalContainer(List<String> problems) {
    if (!CONTAINER_NAME.matcher(portalContainer).matches()) {
      problems.add("invalid portal container name");
      return;
    }
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      Set<ObjectName> names = server.queryNames(new ObjectName("exo:container=portal,name=" + portalContainer + ",*"), null);
      if (names.isEmpty()) {
        problems.add("portal container '" + portalContainer + "' is not registered");
      } else {
        for (ObjectName name : names) {
          if (!Boolean.TRUE.equals(server.getAttribute(name, "Started"))) {
            problems.add("portal container '" + portalContainer + "' is not started");
            break;
          }
        }
      }
    } catch (Exception e) {
      problems.add("portal container '" + portalContainer + "' state is unreadable");
    }
  }
}
