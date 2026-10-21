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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadingPageListenerTest {

  private static final String HTTP = "org.apache.coyote.http11.Http11NioProtocol";

  @TempDir
  Path                        dir;

  private LoadingPageListener listener = new LoadingPageListener();

  private HttpClient          client   = HttpClient.newHttpClient();

  @AfterEach
  void clean() {
    listener.stop();
    System.clearProperty("exo.loading.page");
    System.clearProperty("exo.conf.dir");
    System.clearProperty("catalina.base");
  }

  @Test
  void followsTheLifecycleOfAConnectorWithItsOwnFiles() throws Exception {
    Files.writeString(dir.resolve("branding.properties"), "product.name=Test product\n");
    Files.writeString(dir.resolve("checkpoints.conf"), "kernel=Building root container\n");
    Path log = dir.resolve("platform.log");
    Files.writeString(log, "old content\n");
    listener.setConfDir(dir.toString());
    listener.setLogFile(log.toString());
    int port = freePort();

    listener.lifecycleEvent(new LifecycleEvent(connector(port, "127.0.0.1"), Lifecycle.BEFORE_INIT_EVENT, null));
    assertTrue(get(port, "/").body().contains("Test product"));
    assertFalse(get(port, "/_loading/status").body().contains("kernel"));

    Files.writeString(log, "2026 | INFO  | Building root container [x]\n", StandardOpenOption.APPEND);
    assertTrue(await(() -> status(port).contains("\"checkpoints\":[\"kernel\"]")), status(port));

    listener.lifecycleEvent(new LifecycleEvent(connector(port, "127.0.0.1"), Lifecycle.BEFORE_START_EVENT, null));
    assertPortIsFree(port);
  }

  @Test
  void followsTheDeploymentsOfTheEngineOfTheConnector() throws Exception {
    int port = freePort();
    Connector connector = connector(port, "127.0.0.1");
    org.apache.catalina.core.StandardService service = new org.apache.catalina.core.StandardService();
    service.setContainer(new org.apache.catalina.core.StandardEngine());
    connector.setService(service);
    listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.BEFORE_INIT_EVENT, null));
    assertTrue(status(port).contains("\"phase\":\"starting\""), status(port));
  }

  @Test
  void usesTheDefaultLocationsOfThePlatform() throws Exception {
    System.setProperty("exo.conf.dir", dir.toString());
    System.setProperty("catalina.base", dir.toString());
    Files.createDirectories(dir.resolve("loading"));
    Files.writeString(dir.resolve("loading/branding.properties"), "product.name=From the conf dir\n");
    Files.createDirectories(dir.resolve("logs"));
    Path log = dir.resolve("logs/platform.log");
    Files.writeString(log, "");
    int port = freePort();

    // a connector without address listens on all interfaces
    listener.lifecycleEvent(new LifecycleEvent(connector(port, null), Lifecycle.BEFORE_INIT_EVENT, null));
    assertTrue(get(port, "/").body().contains("From the conf dir"));
    Files.writeString(log, "Building root container\n", StandardOpenOption.APPEND);
    assertTrue(await(() -> status(port).contains("\"kernel\"")), status(port));
  }

  @Test
  void canBeDisabled() throws Exception {
    LoadingPageListener disabled = new LoadingPageListener();
    disabled.setEnabled(false);
    int port = freePort();
    disabled.lifecycleEvent(new LifecycleEvent(connector(port, "127.0.0.1"), Lifecycle.BEFORE_INIT_EVENT, null));
    assertPortIsFree(port);

    for (String flag : new String[] { "false", "FALSE", "0", "no" }) {
      System.setProperty("exo.loading.page", flag);
      listener.lifecycleEvent(new LifecycleEvent(connector(port, "127.0.0.1"), Lifecycle.BEFORE_INIT_EVENT, null));
      assertPortIsFree(port);
    }

    System.setProperty("exo.loading.page", "true");
    listener.lifecycleEvent(new LifecycleEvent(connector(port, "127.0.0.1"), Lifecycle.BEFORE_INIT_EVENT, null));
    assertEquals(503, get(port, "/").statusCode());
  }

  @Test
  void ignoresTheConnectorsThatAreNotPlainHttp() throws Exception {
    int port = freePort();
    Connector ajp = new Connector("AJP/1.3");
    ajp.setPort(port);
    listener.lifecycleEvent(new LifecycleEvent(ajp, Lifecycle.BEFORE_INIT_EVENT, null));
    assertPortIsFree(port);

    Connector secure = connector(port, "127.0.0.1");
    secure.setProperty("SSLEnabled", "true");
    listener.lifecycleEvent(new LifecycleEvent(secure, Lifecycle.BEFORE_INIT_EVENT, null));
    assertPortIsFree(port);

    Connector noPort = connector(0, "127.0.0.1");
    listener.lifecycleEvent(new LifecycleEvent(noPort, Lifecycle.BEFORE_INIT_EVENT, null));
    // an event of another type changes nothing, and so does an initialization that is not the one of a connector
    listener.lifecycleEvent(new LifecycleEvent(connector(port, "127.0.0.1"), Lifecycle.START_EVENT, null));
    listener.lifecycleEvent(new LifecycleEvent(new org.apache.catalina.core.StandardServer(), Lifecycle.BEFORE_INIT_EVENT, null));
    assertPortIsFree(port);
  }

  @Test
  void reportsTheFailureThenClosesWhenTheServerStopsBeforeBeingStarted() throws Exception {
    // long enough to read the status on a slow machine, the port is released once it is over
    listener.setFailureGraceMillis(2000);
    int port = freePort();
    Connector connector = connector(port, "127.0.0.1");
    listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.BEFORE_INIT_EVENT, null));

    listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.AFTER_DESTROY_EVENT, null));
    assertTrue(status(port).contains("\"phase\":\"failed\""), status(port));
    assertTrue(await(() -> isFree(port)), "The port must be released after the delay");

    // nothing to do once it is closed, nor when the connector stops after having been started
    listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.BEFORE_STOP_EVENT, null));
    listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.BEFORE_START_EVENT, null));
  }

  private static Connector connector(int port, String address) {
    Connector connector = new Connector(HTTP);
    connector.setPort(port);
    if (address != null) {
      connector.setProperty("address", address);
    }
    return connector;
  }

  private String status(int port) {
    try {
      return get(port, "/_loading/status").body();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private HttpResponse<String> get(int port, String path) throws Exception {
    return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(), BodyHandlers.ofString());
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      return socket.getLocalPort();
    }
  }

  private static boolean isFree(int port) {
    try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
      return socket.getLocalPort() == port;
    } catch (IOException e) {
      return false;
    }
  }

  private static void assertPortIsFree(int port) {
    assertTrue(isFree(port), "The port " + port + " must not be used");
  }

  /**
   * Waits for a condition that depends on a background thread.
   */
  private static boolean await(BooleanSupplier condition) {
    long limit = System.nanoTime() + 5_000_000_000L;
    while (System.nanoTime() < limit) {
      try {
        if (condition.getAsBoolean()) {
          return true;
        }
      } catch (RuntimeException e) {
        // not ready yet
      }
      LockSupport.parkNanos(25_000_000L);
    }
    return false;
  }

}
