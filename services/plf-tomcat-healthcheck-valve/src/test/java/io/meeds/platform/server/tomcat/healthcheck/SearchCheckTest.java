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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class SearchCheckTest {

  private HttpServer                      server;

  private final AtomicReference<String>   authorization = new AtomicReference<>();

  private final AtomicReference<String>   path          = new AtomicReference<>();

  private volatile int                    status        = 200;

  private volatile String                 body          = "{\"status\":\"green\"}";

  private volatile boolean                slow;

  private final CountDownLatch             release       = new CountDownLatch(1);

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      path.set(exchange.getRequestURI().getPath());
      if (slow) {
        // Waits until the test is over
        try {
          release.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    System.setProperty("exo.es.search.server.url", "http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  void stop() {
    release.countDown();
    server.stop(0);
    System.clearProperty("exo.es.search.server.url");
    System.clearProperty("exo.es.search.server.username");
    System.clearProperty("exo.es.search.server.password");
    Thread.interrupted();
  }

  /** Answers of <code>GET /_cluster/health</code> pasted as Elasticsearch 9.4.0 (single node) returned them: a green cluster. */
  private static final String GREEN  = "{\"cluster_name\":\"docker-cluster\",\"status\":\"green\",\"timed_out\":false,\"number_of_nodes\":1,"
      + "\"number_of_data_nodes\":1,\"active_primary_shards\":0,\"active_shards\":0,\"relocating_shards\":0,"
      + "\"initializing_shards\":0,\"unassigned_shards\":0,\"unassigned_primary_shards\":0,\"delayed_unassigned_shards\":0,"
      + "\"number_of_pending_tasks\":167,\"number_of_in_flight_fetch\":0,\"task_max_waiting_in_queue_millis\":600,"
      + "\"active_shards_percent_as_number\":100.0}";

  /** The same cluster with an index that has a replica it cannot allocate: yellow. */
  private static final String YELLOW = "{\"cluster_name\":\"docker-cluster\",\"status\":\"yellow\",\"timed_out\":false,\"number_of_nodes\":1,"
      + "\"number_of_data_nodes\":1,\"active_primary_shards\":2,\"active_shards\":2,\"relocating_shards\":0,"
      + "\"initializing_shards\":0,\"unassigned_shards\":1,\"unassigned_primary_shards\":0,\"delayed_unassigned_shards\":0,"
      + "\"number_of_pending_tasks\":0,\"number_of_in_flight_fetch\":0,\"task_max_waiting_in_queue_millis\":0,"
      + "\"active_shards_percent_as_number\":66.66666666666666}";

  /** The same cluster with an index whose primary shard cannot be allocated: red. */
  private static final String RED    = "{\"cluster_name\":\"docker-cluster\",\"status\":\"red\",\"timed_out\":false,\"number_of_nodes\":1,"
      + "\"number_of_data_nodes\":1,\"active_primary_shards\":3,\"active_shards\":3,\"relocating_shards\":0,"
      + "\"initializing_shards\":0,\"unassigned_shards\":2,\"unassigned_primary_shards\":1,\"delayed_unassigned_shards\":0,"
      + "\"number_of_pending_tasks\":0,\"number_of_in_flight_fetch\":0,\"task_max_waiting_in_queue_millis\":0,"
      + "\"active_shards_percent_as_number\":60.0}";

  @Test
  void theAnswersOfElasticsearch940AreRead() {
    body = GREEN;
    assertNull(SearchCheck.check(2));
    body = YELLOW;
    assertNull(SearchCheck.check(2));
    body = RED;
    assertEquals("cluster status is red", SearchCheck.check(2));
  }

  @Test
  void aGreenOrYellowClusterIsHealthy() {
    assertNull(SearchCheck.check(2));
    assertEquals("/_cluster/health", path.get());
    body = "{\"cluster_name\":\"exo\",\"status\" : \"yellow\",\"number_of_nodes\":1}";
    assertNull(SearchCheck.check(2));
  }

  @Test
  void aRedClusterIsDown() {
    body = "{\"status\":\"red\"}";
    assertEquals("cluster status is red", SearchCheck.check(2));
  }

  @Test
  void anAnswerThatIsNotAHealthIsDown() {
    body = "<html>a proxy</html>";
    assertEquals("answered without a status", SearchCheck.check(2));
    status = 500;
    assertEquals("answered HTTP 500", SearchCheck.check(2));
    // A redirection is not followed: the address is not the one of the server
    status = 301;
    assertEquals("answered HTTP 301", SearchCheck.check(2));
  }

  @Test
  void aRefusedUserIsReportedWithoutTheCredentials() {
    System.setProperty("exo.es.search.server.username", "root");
    System.setProperty("exo.es.search.server.password", "s3cret");
    status = 401;
    String reason = SearchCheck.check(2);
    assertEquals("refuses the user (it needs the monitor privilege)", reason);
    status = 403;
    assertEquals(reason, SearchCheck.check(2));
    assertFalse(reason.contains("s3cret"));
    assertFalse(reason.contains("127.0.0.1"));
  }

  @Test
  void sendsTheCredentialsOnlyWhenAUserIsConfigured() {
    SearchCheck.check(2);
    assertNull(authorization.get());
    System.setProperty("exo.es.search.server.username", "root");
    System.setProperty("exo.es.search.server.password", "xxxxx");
    SearchCheck.check(2);
    assertEquals("Basic cm9vdDp4eHh4eA==", authorization.get());
    // A user with no password
    System.clearProperty("exo.es.search.server.password");
    SearchCheck.check(2);
    assertEquals("Basic cm9vdDo=", authorization.get());
  }

  @Test
  void aTrailingSlashOfTheAddressIsIgnored() {
    System.setProperty("exo.es.search.server.url", "http://127.0.0.1:" + server.getAddress().getPort() + "//");
    assertNull(SearchCheck.check(2));
    assertEquals("/_cluster/health", path.get());
  }

  @Test
  void aServerThatIsNotThereIsUnreachable() {
    server.stop(0);
    assertEquals("is unreachable", SearchCheck.check(2));
  }

  @Test
  void aServerThatIsTooSlowIsUnreachable() {
    slow = true;
    assertEquals("is unreachable", SearchCheck.check(1));
  }

  @Test
  void aBlankAddressIsTheDefaultOneLikeInThePlatform() {
    System.setProperty("exo.es.search.server.url", "  ");
    // Nothing listens on the default port of the test machine: the address is the default one, not an unsupported one
    String reason = SearchCheck.check(1);
    assertNotEquals("has an unsupported address", reason);
  }

  @Test
  void neverGoesThroughTheProxyOfTheJvm() throws Exception {
    String previousHost = System.getProperty("http.proxyHost");
    String previousPort = System.getProperty("http.proxyPort");
    String previousNonProxy = System.getProperty("http.nonProxyHosts");
    try (java.net.ServerSocket proxy = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      System.setProperty("http.proxyHost", "127.0.0.1");
      System.setProperty("http.proxyPort", String.valueOf(proxy.getLocalPort()));
      // The default exclusions of the JDK cover the loopback addresses: the test would pass on a client that follows the proxy
      System.setProperty("http.nonProxyHosts", "");
      proxy.setSoTimeout(500);
      authorization.set(null);
      System.setProperty("exo.es.search.server.username", "root");
      System.setProperty("exo.es.search.server.password", "s3cret");
      assertNull(SearchCheck.check(2));
      assertEquals("Basic cm9vdDpzM2NyZXQ=", authorization.get(), "the server got the request directly");
      assertThrows(java.net.SocketTimeoutException.class, proxy::accept, "the proxy got nothing");
    } finally {
      restore("http.proxyHost", previousHost);
      restore("http.proxyPort", previousPort);
      restore("http.nonProxyHosts", previousNonProxy);
    }
  }

  @Test
  void aServerThatStallsAfterTheHeadersIsStoppedByTheCallerNotByTheTimeout() {
    server.createContext("/stall", exchange -> {
      exchange.sendResponseHeaders(200, 1000);
      exchange.getResponseBody().write('{');
      exchange.getResponseBody().flush();
      try {
        release.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      exchange.close();
    });
    System.setProperty("exo.es.search.server.url", "http://127.0.0.1:" + server.getAddress().getPort() + "/stall");
    Thread caller = Thread.currentThread();
    java.util.Timer timer = new java.util.Timer(true);
    // The request timeout (1 s) only bounds the headers: the deadline of DependencyHealth interrupts the caller
    timer.schedule(new java.util.TimerTask() {
      @Override
      public void run() {
        caller.interrupt();
      }
    }, 2000);
    try {
      assertEquals("did not answer in time", SearchCheck.check(1));
    } finally {
      timer.cancel();
    }
  }

  private static void restore(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

  @Test
  void onlyAnHttpAddressIsCalled() {
    for (String url : new String[] { "ftp://127.0.0.1:21", "127.0.0.1:9200", "file:///etc/passwd", "http://[::1" }) {
      System.setProperty("exo.es.search.server.url", url);
      assertEquals("has an unsupported address", SearchCheck.check(1), url);
    }
  }

  @Test
  void aCallerThatIsInterruptedGivesUp() {
    Thread.currentThread().interrupt();
    assertEquals("did not answer in time", SearchCheck.check(2));
    assertTrue(Thread.interrupted());
  }
}
