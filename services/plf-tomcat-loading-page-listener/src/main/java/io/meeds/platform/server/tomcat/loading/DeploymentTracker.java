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
package io.meeds.platform.server.tomcat.loading;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

/**
 * Feeds a {@link StartupProgress} with the web applications deployed by the
 * hosts of an engine: the expected number is counted from the files of each
 * host before the deployment, then each started or failed context is counted.
 */
public final class DeploymentTracker {

  /** Progress reported by the status resource. */
  private final StartupProgress                         progress;

  /** Hosts being followed. */
  private final Set<Host>                               hosts    = ConcurrentHashMap.newKeySet();

  /** Contexts seen: false until their start is settled (started or failed). */
  private final ConcurrentHashMap<Context, Boolean>     contexts = new ConcurrentHashMap<>();

  /** Receives the contexts added to the hosts. */
  private final ContainerListener                       hostListener    = this::hostEvent;

  /** Receives the start and stop of the contexts. */
  private final LifecycleListener                       contextListener = this::contextEvent;

  public DeploymentTracker(StartupProgress startupProgress) {
    this.progress = startupProgress;
  }

  /**
   * Follows every host of the engine.
   *
   * @param engine the engine of the connector service, may be null
   */
  public void attach(Engine engine) {
    if (engine == null) {
      return;
    }
    int total = 0;
    for (Container child : engine.findChildren()) {
      if (child instanceof Host host) {
        total += countExpected(host);
        host.addContainerListener(hostListener);
        hosts.add(host);
      }
    }
    progress.setTotal(total);
  }

  /**
   * Stops following the hosts and contexts.
   */
  public void detach() {
    hosts.forEach(host -> host.removeContainerListener(hostListener));
    hosts.clear();
    contexts.keySet().forEach(context -> context.removeLifecycleListener(contextListener));
    contexts.clear();
  }

  /**
   * Counts the web applications Tomcat is going to deploy: war files,
   * directories and context descriptors, a name being counted once.
   *
   * @param  host the host to scan
   * @return      the number of expected web applications
   */
  static int countExpected(Host host) {
    Set<String> names = new HashSet<>();
    collect(directoryOf(host::getAppBaseFile), names, true);
    collect(directoryOf(host::getConfigBaseFile), names, false);
    return names.size();
  }

  /**
   * @param  directory how to get the folder from the host
   * @return           the folder, null when the host is not fully configured
   */
  private static File directoryOf(Supplier<File> directory) {
    try {
      return directory.get();
    } catch (RuntimeException e) { // NOSONAR the host may not be fully configured
      return null;
    }
  }

  private static void collect(File directory, Set<String> names, boolean appBase) {
    File[] files = directory == null ? null : directory.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      String name = file.getName();
      if (appBase && file.isDirectory()) {
        if (!"META-INF".equalsIgnoreCase(name) && !"WEB-INF".equalsIgnoreCase(name)) {
          names.add(name);
        }
      } else if (appBase && name.toLowerCase().endsWith(".war")) {
        names.add(name.substring(0, name.length() - ".war".length()));
      } else if (!appBase && name.toLowerCase().endsWith(".xml")) {
        names.add(name.substring(0, name.length() - ".xml".length()));
      }
    }
  }

  private void hostEvent(ContainerEvent event) {
    if (Container.ADD_CHILD_EVENT.equals(event.getType()) && event.getData() instanceof Context context
        && contexts.putIfAbsent(context, Boolean.FALSE) == null) {
      context.addLifecycleListener(contextListener);
      progress.applicationSeen();
    }
  }

  private void contextEvent(LifecycleEvent event) {
    if (!(event.getLifecycle() instanceof Context context)) {
      return;
    }
    String type = event.getType();
    if (Lifecycle.AFTER_START_EVENT.equals(type) && contexts.replace(context, Boolean.FALSE, Boolean.TRUE)) {
      progress.applicationStarted();
    } else if (Lifecycle.BEFORE_STOP_EVENT.equals(type) && contexts.replace(context, Boolean.FALSE, Boolean.TRUE)) {
      // Stopped before being started: the start failed
      progress.applicationFailed();
    }
  }

}
