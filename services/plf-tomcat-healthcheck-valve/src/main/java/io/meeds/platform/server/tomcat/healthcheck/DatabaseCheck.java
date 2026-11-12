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

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.Server;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;

/**
 * Checks that the databases answer: every <code>javax.sql.DataSource</code>
 * declared in the global naming resources of the server (the pools of
 * <code>server.xml</code> the platform uses) gives a valid connection.
 */
final class DatabaseCheck {

  /** Logger of the check. */
  private static final Log LOG = LogFactory.getLog(DatabaseCheck.class);

  /** Seconds the driver may take to validate a connection. */
  private static final int VALIDATION_SECONDS = 2;

  /** Shortest time a pool must stay saturated to be reported, when its <code>maxWait</code> is smaller. */
  private static final long MIN_SATURATION_MILLIS = 1000;

  /** Milliseconds in a second. */
  private static final long MILLIS_PER_SECOND = 1000;

  /**
   * When each saturated pool was seen saturated with no connection back since,
   * by pool: a pool that is gone is not kept.
   */
  private static final Map<DataSource, Long> SATURATED_SINCE = Collections.synchronizedMap(new WeakHashMap<>());

  /** The returned count of each saturated pool at that sighting, by pool. */
  private static final Map<DataSource, Long> RETURNED_AT_SIGHTING = Collections.synchronizedMap(new WeakHashMap<>());

  private DatabaseCheck() {
    // Utility class
  }

  /**
   * @param server the server whose global naming resources are read
   * @return the data sources by name, empty when the server has no naming
   *         context or none is a data source
   * @throws NamingException when a data source can not be listed or created
   */
  static Map<String, DataSource> sources(Server server) throws NamingException {
    Map<String, DataSource> sources = new TreeMap<>();
    Context naming = server.getGlobalNamingContext();
    if (naming == null) {
      return sources;
    }
    NamingEnumeration<Binding> bindings = naming.listBindings("");
    while (bindings.hasMore()) {
      Binding binding = bindings.next();
      // The class name Tomcat reports is the one of the object it created (the pool), not javax.sql.DataSource
      if (binding.getObject() instanceof DataSource source) {
        sources.put(binding.getName(), source);
      }
    }
    return sources;
  }

  /**
   * A pool whose connections are all in use is busy, not down: borrowing from
   * it would wait for <code>maxWait</code> (longer than the budget of the
   * check) and join the queue of the waiters, so it is not asked. But a
   * pool that stays saturated is not busy any more: when no connection comes
   * back, as when the database stops answering without closing them, it is
   * reported once it has been saturated for <code>maxWait</code> across the
   * evaluations. A connection that came back between two evaluations (the
   * returned count of the pool moved) starts the time again, so two sightings
   * of a busy pool are not taken for a pool that stayed saturated.
   *
   * @param source the data source to check
   * @return null when it gives a valid connection or is saturated for less
   *         than <code>maxWait</code>, the reason otherwise
   */
  static String check(DataSource source) {
    return check(source, System.nanoTime());
  }

  /**
   * @param source the data source to check
   * @param nowNanos the current time, as given by {@link System#nanoTime()}
   * @return null when it gives a valid connection or is saturated for less
   *         than <code>maxWait</code>, the reason otherwise
   */
  static String check(DataSource source, long nowNanos) {
    if (source instanceof DataSourceProxy pool) {
      if (pool.getActive() >= pool.getMaxActive()) {
        return saturated(pool, source, nowNanos);
      }
      SATURATED_SINCE.remove(source);
      RETURNED_AT_SIGHTING.remove(source);
    }
    try (Connection connection = source.getConnection()) {
      return connection.isValid(VALIDATION_SECONDS) ? null : "gives an invalid connection";
    } catch (Exception e) {
      // Only the kind of failure: the message of a driver may hold the address of the database
      LOG.warn("A data source does not give a connection: " + e.getClass().getSimpleName());
      return "is unreachable";
    }
  }

  private static String saturated(DataSourceProxy pool, DataSource source, long nowNanos) {
    long maxWaitMillis = Math.max(pool.getMaxWait(), MIN_SATURATION_MILLIS);
    // Two busy moments are not a saturation: when a connection came back between the sightings, the count starts again
    long returned = pool.getReturnedCount();
    Long previous = RETURNED_AT_SIGHTING.get(source);
    if (previous == null || previous != returned) {
      SATURATED_SINCE.put(source, nowNanos);
      RETURNED_AT_SIGHTING.put(source, returned);
    }
    long since = SATURATED_SINCE.get(source);
    long millis = TimeUnit.NANOSECONDS.toMillis(nowNanos - since);
    if (millis >= maxWaitMillis) {
      LOG.warn("A data source has had every connection in use for " + millis / MILLIS_PER_SECOND + " s");
      return "has had every connection in use for over " + (maxWaitMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND + " s";
    }
    LOG.debug("A data source has all its connections in use, it is not asked");
    return null;
  }
}
