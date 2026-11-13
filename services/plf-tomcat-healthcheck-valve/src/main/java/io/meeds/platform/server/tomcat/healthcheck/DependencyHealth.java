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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.Server;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Checks what the platform depends on once it is started: its databases and
 * its search server. Each check runs in its own thread and is given a few
 * seconds, so that a dependency that hangs makes the answer a failure instead
 * of making the probe hang. The result is kept for a few seconds: however
 * often the health check is called, the dependencies are asked once in that
 * time.
 */
final class DependencyHealth {

  /** Name, in the configuration, of the databases check. */
  static final String      DATABASE        = "database";

  /** Name, in the configuration, of the search server check. */
  static final String      SEARCH          = "search";

  /** Logger of the checks. */
  private static final Log LOG             = LogFactory.getLog(DependencyHealth.class);

  /** Seconds each dependency is given to answer. */
  private static final int TIMEOUT_SECONDS = 3;

  /**
   * Threads the checks may use together. A check that does not end (a driver
   * stuck in a blocking read does not react to an interruption) keeps its thread:
   * with no limit, every evaluation during a long outage would add some.
   */
  private static final int MAX_THREADS     = 16;

  /** Seconds a thread that has nothing to check is kept. */
  private static final long KEEP_ALIVE_SECONDS = 30;

  /** Name of the threads that run the checks. */
  private static final String THREAD_NAME  = "exo-healthcheck-";

  /** Whether the databases are checked. */
  private final boolean         checkDatabase;

  /** Whether the search server is checked. */
  private final boolean         checkSearch;

  /** Nanoseconds a result is kept. */
  private final long            cacheNanos;

  /** Seconds each dependency is given to answer. */
  private final int             timeoutSeconds;

  /** Runs the checks. */
  private final ExecutorService executor;

  /** Result of the last check. */
  private List<String>          last      = List.of();

  /** Time of the last check, when there was one. */
  private long                  checkedAt;

  /** Whether a check was made. */
  private boolean               checked;

  /**
   * @param kinds        the dependencies to check, {@link #DATABASE} and/or
   *                     {@link #SEARCH}
   * @param cacheSeconds seconds a result is kept, 0 to check on every call
   */
  DependencyHealth(Set<String> kinds, int cacheSeconds) {
    this(kinds, cacheSeconds, TIMEOUT_SECONDS, MAX_THREADS);
  }

  /**
   * @param kinds        the dependencies to check
   * @param cacheSeconds seconds a result is kept, 0 to check on every call
   * @param timeout      seconds each dependency is given to answer
   * @param maxThreads   threads the checks may use together
   */
  DependencyHealth(Set<String> kinds, int cacheSeconds, int timeout, int maxThreads) {
    this.checkDatabase = kinds.contains(DATABASE);
    this.checkSearch = kinds.contains(SEARCH);
    this.cacheNanos = TimeUnit.SECONDS.toNanos(cacheSeconds);
    this.timeoutSeconds = timeout;
    AtomicInteger count = new AtomicInteger();
    // No queue: a check starts at once or is refused, it never waits behind a stuck one
    this.executor = new ThreadPoolExecutor(0, maxThreads, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new SynchronousQueue<>(), task -> {
      Thread thread = new Thread(task, THREAD_NAME + count.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    });
  }

  /**
   * @param value the configured value: <code>true</code> or empty for every
   *              check, <code>false</code>, <code>none</code>, <code>0</code>
   *              or <code>no</code> for none, otherwise a comma separated list
   *              of {@link #DATABASE} and {@link #SEARCH}
   * @return the dependencies to check
   */
  static Set<String> parse(String value) {
    String text = value == null ? "true" : value.trim().toLowerCase(Locale.ROOT);
    if (text.isEmpty() || "true".equals(text)) {
      return Set.of(DATABASE, SEARCH);
    }
    Set<String> kinds = new HashSet<>();
    for (String item : text.split(",")) {
      String name = item.trim();
      if (DATABASE.equals(name) || SEARCH.equals(name)) {
        kinds.add(name);
      } else if (!List.of("false", "none", "0", "no").contains(name)) {
        LOG.error("Unknown health check dependency '" + name + "', expected " + DATABASE + " or " + SEARCH);
      }
    }
    return kinds;
  }

  /**
   * @return true when at least one dependency is checked
   */
  boolean isEnabled() {
    return checkDatabase || checkSearch;
  }

  /**
   * @param server the server whose data sources are checked
   * @return the dependencies that do not answer, empty when they all do
   */
  synchronized List<String> problems(Server server) {
    long now = System.nanoTime();
    if (checked && now - checkedAt < cacheNanos) {
      return last;
    }
    last = check(server);
    checkedAt = System.nanoTime();
    checked = true;
    return last;
  }

  /**
   * @return the threads the checks hold now
   */
  int threads() {
    return ((ThreadPoolExecutor) executor).getPoolSize();
  }

  /** Stops the threads of the checks. */
  void close() {
    executor.shutdownNow();
  }

  private List<String> check(Server server) {
    List<String> problems = new ArrayList<>();
    Map<String, Callable<String>> tasks = new LinkedHashMap<>();
    if (checkDatabase) {
      try {
        for (Map.Entry<String, DataSource> source : DatabaseCheck.sources(server).entrySet()) {
          tasks.put("database '" + source.getKey() + "'", () -> DatabaseCheck.check(source.getValue()));
        }
      } catch (NamingException e) {
        LOG.warn("The data sources of the server can not be read: " + e.getMessage());
        problems.add("database resources can not be read");
      }
    }
    if (checkSearch) {
      tasks.put("search server", () -> SearchCheck.check(timeoutSeconds));
    }
    Map<String, Future<String>> futures = new LinkedHashMap<>();
    tasks.forEach((label, task) -> {
      try {
        futures.put(label, executor.submit(task));
      } catch (RejectedExecutionException e) {
        // Every thread is held by a check of an earlier evaluation that does not end
        problems.add(label + " is still not answering");
      }
    });
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    futures.forEach((label, future) -> {
      String reason = await(label, future, deadline);
      if (reason != null) {
        problems.add(label + " " + reason);
      }
    });
    return List.copyOf(problems);
  }

  private String await(String label, Future<String> future, long deadline) {
    try {
      return future.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      return "did not answer within " + timeoutSeconds + " s";
    } catch (InterruptedException | ExecutionException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      LOG.warn("The check of the " + label + " did not complete: " + e.getClass().getSimpleName());
      return "can not be checked";
    }
  }
}
