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

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the listener in a real Tomcat. The feature relies on the order in
 * which Tomcat starts its components: the listener of a connector is attached
 * by the configuration, the connector is started after the hosts deployed
 * their applications, a context is added to its host before it is started, and
 * the port is free for the connector once the page stopped. A Tomcat that
 * changes any of these makes this test fail, where the other tests, that fire
 * the events by hand, would not.
 */
class LoadingPageHandoffTest {

  private static final int TIMEOUT_SECONDS = 60;

  @TempDir
  Path                     dir;

  private final HttpClient http    = HttpClient.newHttpClient();

  /** Counted down when the web application of the test is starting. */
  private final CountDownLatch starting = new CountDownLatch(1);

  /** Counted down by the test to let the web application finish its start. */
  private final CountDownLatch release  = new CountDownLatch(1);

  private ExecutorService      executor = Executors.newSingleThreadExecutor();

  private Tomcat               tomcat;

  @AfterEach
  void clean() throws Exception {
    release.countDown();
    executor.shutdownNow();
    if (tomcat != null) {
      tomcat.stop();
      tomcat.destroy();
    }
  }

  @Test
  void theLoadingPageAnswersWhileAnApplicationStartsThenTomcatTakesThePort() throws Exception {
    int port = freePort();
    Files.createDirectories(dir.resolve("conf"));
    Files.createDirectories(dir.resolve("webapp"));
    tomcat = new Tomcat();
    tomcat.setBaseDir(dir.resolve("base").toString());
    tomcat.setHostname("localhost");
    tomcat.setPort(port);
    Connector connector = tomcat.getConnector();
    connector.setProperty("address", "127.0.0.1");
    connector.setProperty("bindOnInit", "false");
    LoadingPageListener listener = new LoadingPageListener();
    listener.setConfDir(dir.resolve("conf").toString());
    listener.setLogFile(dir.resolve("platform.log").toString());
    connector.addLifecycleListener(listener);
    // The way HostConfig does: the host deploys its applications synchronously when it starts
    tomcat.getHost().addLifecycleListener(event -> {
      if (Lifecycle.START_EVENT.equals(event.getType())) {
        deployAnApplicationThatTakesItsTime();
      }
    });

    Future<Void> start = executor.submit(() -> {
      tomcat.start();
      return null;
    });
    assertTrue(starting.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the application never started");

    HttpResponse<String> page = get(port, "/");
    assertEquals(503, page.statusCode());
    assertTrue(page.headers().firstValue(LoadingPageServer.MARKER_HEADER).isPresent(), page.headers().toString());
    HttpResponse<String> status = get(port, "/_loading/status");
    assertTrue(status.body().contains("\"phase\":\"deploying\""), status.body());

    release.countDown();
    start.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    // Tomcat holds the port now: nothing answers as the loading page, an unknown path is Tomcat's own 404
    HttpResponse<String> served = get(port, "/");
    assertEquals(404, served.statusCode());
    assertFalse(served.headers().firstValue(LoadingPageServer.MARKER_HEADER).isPresent(), served.headers().toString());
  }

  private void deployAnApplicationThatTakesItsTime() {
    StandardContext context = new StandardContext();
    context.setName("/slow");
    context.setPath("/slow");
    context.setDocBase(dir.resolve("webapp").toString());
    context.addLifecycleListener(new Tomcat.FixContextListener());
    context.addLifecycleListener(event -> {
      if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
        starting.countDown();
        try {
          release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    tomcat.getHost().addChild(context);
  }

  private HttpResponse<String> get(int port, String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                     HttpResponse.BodyHandlers.ofString());
  }

  private static int freePort() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

}
