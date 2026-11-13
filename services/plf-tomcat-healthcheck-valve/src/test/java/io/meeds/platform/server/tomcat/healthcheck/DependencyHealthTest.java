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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DependencyHealthTest {

  private final AtomicInteger calls = new AtomicInteger();

  /** Never counted down: a hanging data source only ends when it is interrupted. */
  private final CountDownLatch release = new CountDownLatch(1);

  private DependencyHealth    health;

  @AfterEach
  void stop() {
    release.countDown();
    if (health != null) {
      health.close();
    }
    Thread.interrupted();
    System.clearProperty("exo.es.search.server.url");
  }

  @Test
  void readsWhichDependenciesToCheck() {
    for (String value : new String[] { null, "", " true ", "TRUE" }) {
      assertEquals(Set.of("database", "search"), DependencyHealth.parse(value), String.valueOf(value));
    }
    assertEquals(Set.of("database"), DependencyHealth.parse("database"));
    assertEquals(Set.of("database", "search"), DependencyHealth.parse(" Search , database "));
    for (String value : new String[] { "false", "none", "0", "no", "NO" }) {
      assertEquals(Set.of(), DependencyHealth.parse(value), value);
    }
    // An unknown name is logged and ignored
    assertEquals(Set.of("search"), DependencyHealth.parse("search,databse"));
  }

  @Test
  void nothingIsCheckedWhenNothingIsConfigured() {
    assertFalse(new DependencyHealth(Set.of(), 0).isEnabled());
    assertTrue(new DependencyHealth(Set.of("search"), 0).isEnabled());
    assertTrue(new DependencyHealth(Set.of("database"), 0).isEnabled());
  }

  @Test
  void aServerWithoutNamingHasNoDataSource() throws Exception {
    health = new DependencyHealth(Set.of("database"), 0);
    assertEquals(List.of(), health.problems(server(null)));
    assertEquals(Map.of(), DatabaseCheck.sources(server(null)));
  }

  @Test
  void checksEveryDataSourceAndIgnoresTheOtherResources() {
    health = new DependencyHealth(Set.of("database"), 0);
    Context naming = naming(Map.of("jpa", dataSource(Mode.VALID), "idm", dataSource(Mode.VALID)), Map.of("realm", "a user database"));
    assertEquals(List.of(), health.problems(server(naming)));
    assertEquals(2, calls.get());
  }

  @Test
  void reportsEachDataSourceThatFails() {
    health = new DependencyHealth(Set.of("database"), 0);
    Context naming = naming(Map.of("a_ok", dataSource(Mode.VALID), "b_invalid", dataSource(Mode.INVALID), "c_down", dataSource(Mode.DOWN)),
                            Map.of());
    assertEquals(List.of("database 'b_invalid' gives an invalid connection", "database 'c_down' is unreachable"),
                 health.problems(server(naming)));
  }

  @Test
  void aDataSourceThatHangsDoesNotHangTheAnswer() {
    health = new DependencyHealth(Set.of("database"), 0, 1, 16);
    Context naming = naming(Map.of("slow", dataSource(Mode.HANG), "fast", dataSource(Mode.VALID)), Map.of());
    long start = System.nanoTime();
    List<String> problems = health.problems(server(naming));
    assertEquals(List.of("database 'slow' did not answer within 1 s"), problems);
    assertTrue(System.nanoTime() - start < 4_000_000_000L, "answers after the timeout, not after the hang");
  }

  @Test
  void aCheckThatNeverEndsDoesNotLetThreadsPileUp() {
    health = new DependencyHealth(Set.of("database"), 0, 1, 2);
    Server server = server(naming(Map.of("stuck", dataSource(Mode.STUCK)), Map.of()));
    List<String> timedOut = List.of("database 'stuck' did not answer within 1 s");
    assertEquals(timedOut, health.problems(server));
    assertEquals(timedOut, health.problems(server));
    // Both threads are held by checks that ignore their interruption: the next evaluation is refused at once
    long start = System.nanoTime();
    assertEquals(List.of("database 'stuck' is still not answering"), health.problems(server));
    assertTrue(System.nanoTime() - start < 500_000_000L, "refused at once, not after the timeout");
    assertEquals(2, health.threads());
    assertEquals(List.of("database 'stuck' is still not answering"), health.problems(server));
    assertEquals(2, health.threads());
  }

  @Test
  void aDataSourceThatCanNotBeReadIsAProblem() {
    health = new DependencyHealth(Set.of("database"), 0);
    Context failing = (Context) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Context.class }, (proxy, method, args) -> {
      throw new NamingException("The factory failed");
    });
    assertEquals(List.of("database resources can not be read"), health.problems(server(failing)));
  }

  @Test
  void keepsTheResultForTheCacheDuration() {
    health = new DependencyHealth(Set.of("database"), 60);
    Context naming = naming(Map.of("jpa", dataSource(Mode.VALID)), Map.of());
    Server server = server(naming);
    assertEquals(List.of(), health.problems(server));
    assertEquals(List.of(), health.problems(server));
    assertEquals(1, calls.get());
    health.close();
    health = new DependencyHealth(Set.of("database"), 0);
    health.problems(server);
    health.problems(server);
    assertEquals(3, calls.get());
  }

  @Test
  void aCallerThatIsInterruptedGivesUp() {
    health = new DependencyHealth(Set.of("database"), 0);
    Context naming = naming(Map.of("slow", dataSource(Mode.HANG)), Map.of());
    Thread.currentThread().interrupt();
    assertEquals(List.of("database 'slow' can not be checked"), health.problems(server(naming)));
    assertTrue(Thread.interrupted());
  }

  @Test
  void checksTheSearchServerAndTheDatabasesTogether() {
    System.setProperty("exo.es.search.server.url", "ftp://127.0.0.1");
    health = new DependencyHealth(Set.of("database", "search"), 0);
    Context naming = naming(Map.of("jpa", dataSource(Mode.DOWN)), Map.of());
    assertEquals(List.of("database 'jpa' is unreachable", "search server has an unsupported address"), health.problems(server(naming)));
  }

  private enum Mode {
    VALID, INVALID, DOWN, HANG, STUCK
  }

  private DataSource dataSource(Mode mode) {
    return (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { DataSource.class }, (proxy, method, args) -> {
      calls.incrementAndGet();
      if (mode == Mode.DOWN) {
        throw new SQLException("Connection refused");
      }
      if (mode == Mode.HANG) {
        // Waits until the test releases it, or until the check gives up and interrupts it
        release.await();
      }
      if (mode == Mode.STUCK) {
        // Ignores the interruption, like a driver stuck in a blocking read: it only ends when the test releases it
        while (release.getCount() > 0) {
          try {
            release.await();
          } catch (InterruptedException e) {
            // Keeps waiting
          }
        }
      }
      return connection(mode == Mode.VALID);
    });
  }

  private Connection connection(boolean valid) {
    return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class }, (proxy, method, args) -> {
      return "isValid".equals(method.getName()) ? Boolean.valueOf(valid) : null;
    });
  }

  private Server server(Context naming) {
    return (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Server.class }, (proxy, method, args) -> naming);
  }

  /** A naming context listing the data sources and the other resources. */
  private Context naming(Map<String, DataSource> sources, Map<String, String> others) {
    List<Binding> bindings = new ArrayList<>();
    sources.forEach((name, source) -> bindings.add(new Binding(name, source)));
    others.forEach((name, value) -> bindings.add(new Binding(name, value)));
    return (Context) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Context.class }, (proxy, method, args) -> {
      if ("listBindings".equals(method.getName())) {
        return enumeration(bindings);
      }
      throw new IllegalStateException(method.getName());
    });
  }

  private NamingEnumeration<Binding> enumeration(List<Binding> bindings) {
    java.util.Iterator<Binding> iterator = bindings.iterator();
    return new NamingEnumeration<>() {
      @Override
      public Binding next() {
        return iterator.next();
      }

      @Override
      public boolean hasMore() {
        return iterator.hasNext();
      }

      @Override
      public void close() {
        // Nothing to release
      }

      @Override
      public boolean hasMoreElements() {
        return iterator.hasNext();
      }

      @Override
      public Binding nextElement() {
        return iterator.next();
      }
    };
  }
}
