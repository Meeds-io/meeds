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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * What is known about the progress of the startup: how many web applications
 * are expected, how many are started or failed. It only holds counters, no
 * application name is exposed since the loading page is public.
 */
public final class StartupProgress {

  /** Name of the phase while no web application has been seen yet. */
  public static final String PHASE_STARTING  = "starting";

  /** Name of the phase while web applications are deployed. */
  public static final String PHASE_DEPLOYING = "deploying";

  /** Name of the phase once every web application is deployed. */
  public static final String PHASE_SERVICES  = "services";

  /** Name of the phase when the server is stopping instead of starting. */
  public static final String PHASE_FAILED    = "failed";

  /** Number of web applications expected. */
  private volatile int       total;

  /** Number of web applications being deployed or deployed. */
  private final AtomicInteger seen            = new AtomicInteger();

  /** Number of web applications started. */
  private final AtomicInteger started         = new AtomicInteger();

  /** Number of web applications that failed to start. */
  private final AtomicInteger failed          = new AtomicInteger();

  /** True once the server is stopping before being started. */
  private volatile boolean    stopping;

  /** Checkpoints of the startup, in their display order. */
  private final AtomicReference<List<Checkpoint>> checkpoints = new AtomicReference<>(List.of());

  /** Reader of the platform log, null when the log is not followed. */
  private final AtomicReference<CheckpointTracker> logTracker = new AtomicReference<>();

  /**
   * @param expected number of web applications expected, 0 when unknown
   */
  public void setTotal(int expected) {
    this.total = Math.max(0, expected);
  }

  /**
   * @param list    checkpoints of the startup, in their display order
   * @param tracker reader of the platform log that finds them, may be null
   */
  public void setCheckpoints(List<Checkpoint> list, CheckpointTracker tracker) {
    this.checkpoints.set(list);
    this.logTracker.set(tracker);
  }

  /**
   * The server is stopping before it was started: the startup failed.
   */
  public void fail() {
    stopping = true;
  }

  /**
   * A web application is being deployed.
   */
  public void applicationSeen() {
    seen.incrementAndGet();
  }

  /**
   * A web application is started.
   */
  public void applicationStarted() {
    started.incrementAndGet();
  }

  /**
   * A web application failed to start.
   */
  public void applicationFailed() {
    failed.incrementAndGet();
  }

  /**
   * The phase deduced from the counters: starting until a web application is
   * seen, deploying until all the expected ones are processed, then services
   * (the portal containers and the platform services are starting).
   *
   * @return one of the PHASE constants
   */
  public String getPhase() {
    return stopping ? PHASE_FAILED : deploymentPhase();
  }

  private String deploymentPhase() {
    int processed = started.get() + failed.get();
    if (seen.get() == 0) {
      return PHASE_STARTING;
    } else if (total <= 0 || processed < total) {
      return PHASE_DEPLOYING;
    }
    return PHASE_SERVICES;
  }

  /**
   * @param  elapsedMillis time spent since the loading page started
   * @return               the status as JSON
   */
  public String toJson(long elapsedMillis) {
    return "{\"elapsed\":" + elapsedMillis
        + ",\"phase\":\"" + getPhase() + "\""
        + ",\"deployed\":" + started.get()
        + ",\"failed\":" + failed.get()
        + ",\"total\":" + total
        + ",\"checkpoints\":[" + reachedCheckpoints() + "]}";
  }

  private String reachedCheckpoints() {
    StringBuilder ids = new StringBuilder();
    boolean deployed = PHASE_SERVICES.equals(deploymentPhase());
    CheckpointTracker tracker = logTracker.get();
    for (Checkpoint checkpoint : checkpoints.get()) {
      boolean reached = Checkpoint.START.equals(checkpoint.builtin())
          || Checkpoint.DEPLOYED.equals(checkpoint.builtin()) && deployed
          || tracker != null && tracker.isReached(checkpoint.id());
      if (reached) {
        ids.append(ids.isEmpty() ? "" : ",").append('"').append(checkpoint.id()).append('"');
      }
    }
    return ids.toString();
  }

}
