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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class HealthCheckValveTest {

  private static final ObjectName PORTAL = name("exo:container=portal,name=portal");

  /** Counts the sessions the web application got to create. */
  static final AtomicInteger      SESSIONS = new AtomicInteger();

  private Tomcat                  tomcat;

  private HealthCheckValve        valve    = new HealthCheckValve();

  private boolean                 inEngine;

  private boolean                 brokenRoot;

  private boolean                 withDataSource;

  /** Mode of the data source created by {@link FakeDataSourceFactory}. */
  static final AtomicReference<String> DATA_SOURCE_MODE = new AtomicReference<>("valid");

  private volatile String         searchBody;

  private final AtomicInteger     searchCalls = new AtomicInteger();

  private final HttpClient        client   = HttpClient.newHttpClient();

  @BeforeEach
  void start() {
    SESSIONS.set(0);
    inEngine = false;
    brokenRoot = false;
    withDataSource = false;
    searchCalls.set(0);
    // The tests must not depend on the variables exported by the shell that runs them
    System.setProperty("exo.healthcheck", "true");
    valve.setAllow("127.0.0.1,::1");
    valve.setPortalContainer("none");
    // The dependencies are checked by the tests that set them
    valve.setDeep("none");
    valve.setCacheSeconds(0);
  }

  @AfterEach
  void stop() throws Exception {
    if (tomcat != null) {
      tomcat.stop();
      tomcat.destroy();
    }
    System.clearProperty("exo.healthcheck");
    System.clearProperty("exo.healthcheck.allow");
    System.clearProperty("exo.healthcheck.container");
    System.clearProperty("exo.healthcheck.deep");
    System.clearProperty("exo.es.search.server.url");
    unregister();
  }

  @Test
  void answersUpWithoutSessionNorCookieNorTheWebApplication() throws Exception {
    startTomcat(false);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(200, response.statusCode());
    assertEquals("{\"status\":\"UP\"}", response.body());
    assertEquals("application/json;charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
    assertTrue(response.headers().firstValue("Set-Cookie").isEmpty());
    assertEquals(0, SESSIONS.get());
    assertEquals(200, send("HEAD", "/healthcheck").statusCode());
    assertEquals("", send("HEAD", "/healthcheck").body());
  }

  @Test
  void everyAnswerIsNotCached() throws Exception {
    startTomcat(false);
    assertEquals("no-store", get("/healthcheck").headers().firstValue("Cache-Control").orElse(""));
    assertEquals("no-store", send("HEAD", "/healthcheck").headers().firstValue("Cache-Control").orElse(""));
    assertEquals("no-store", send("POST", "/healthcheck").headers().firstValue("Cache-Control").orElse(""));
  }

  @Test
  void aRefusedAnswerIsNotCachedEither() throws Exception {
    valve.setAllow("10.0.0.0/8");
    startTomcat(false);
    HttpResponse<String> refused = get("/healthcheck");
    assertEquals(403, refused.statusCode());
    assertEquals("no-store", refused.headers().firstValue("Cache-Control").orElse(""));
  }

  @Test
  void aBlankSettingIsTheDefaultOne() throws Exception {
    valve = new HealthCheckValve();
    // An empty variable of a compose file: it must not refuse everyone
    System.setProperty("exo.healthcheck.allow", "");
    System.setProperty("exo.healthcheck.container", " ");
    System.setProperty("exo.healthcheck.deep", "false");
    register(true);
    startTomcat(false);
    assertEquals(200, get("/healthcheck").statusCode());
  }

  @Test
  void theDependenciesAreAskedAgainWhenTheHostIsStartedAfterAStop() throws Exception {
    valve.setDeep("search");
    com.sun.net.httpserver.HttpServer search = fakeSearch("{\"status\":\"green\"}");
    try {
      startTomcat(false);
      assertEquals(200, get("/healthcheck").statusCode());
      tomcat.getHost().stop();
      tomcat.getHost().start();
      assertEquals(200, get("/healthcheck").statusCode());
    } finally {
      search.stop(0);
    }
  }

  @Test
  void otherPathsStillReachTheWebApplication() throws Exception {
    startTomcat(false);
    HttpResponse<String> response = get("/app/page");
    assertEquals("app", response.body());
    assertEquals(1, SESSIONS.get());
  }

  @ParameterizedTest(name = "allow {0} answers {1}")
  @CsvSource({ "'10.0.0.0/8',403", "'127.0.0.0/8',200", "'not-an-ip,10.0.0.0/8',200" })
  void theAllowListDecidesWhoIsAnswered(String allow, int status) throws Exception {
    // An invalid list does not stop the server and keeps the default, local callers only
    valve.setAllow(allow);
    startTomcat(false);
    assertEquals(status, get("/healthcheck").statusCode());
  }

  @ParameterizedTest(name = "{0} is refused even from localhost")
  @ValueSource(strings = { "X-Forwarded-For", "Forwarded", "X-Real-IP", "X-Forwarded-Proto", "X-Forwarded-Host", "X-Forwarded-By",
      "Via" })
  void refusesRequestsForwardedByAProxyEvenFromLocalhost(String header) throws Exception {
    startTomcat(false);
    HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/healthcheck"))
                                                           .header(header, "203.0.113.7")
                                                           .build(),
                                                BodyHandlers.ofString());
    assertEquals(403, response.statusCode());
  }

  @Test
  void quotesEveryTextOfTheProblemsAsJson() {
    assertEquals("\"a\\\"b\",\"c\\\\d\",\"e\\u000af\\u0000g\\u001fh\"",
                 HealthCheckValve.quoted(java.util.List.of("a\"b", "c\\d", "e\nf\u0000g\u001fh")));
    assertEquals("", HealthCheckValve.quoted(java.util.List.of()));
  }

  @Test
  void onlyAnswersGetAndHead() throws Exception {
    startTomcat(false);
    HttpResponse<String> response = send("POST", "/healthcheck");
    assertEquals(405, response.statusCode());
    assertEquals("GET, HEAD", response.headers().firstValue("Allow").orElse(""));
  }

  @Test
  void answersOnTheConfiguredPathOnly() throws Exception {
    valve.setPath("/status");
    startTomcat(false);
    assertEquals(200, get("/status").statusCode());
    assertEquals(404, get("/healthcheck").statusCode());
  }

  @Test
  void settingsComeFromTheSystemPropertiesWhenTheValveHasNone() throws Exception {
    valve = new HealthCheckValve();
    System.setProperty("exo.healthcheck.allow", "10.0.0.0/8");
    System.setProperty("exo.healthcheck.container", "none");
    startTomcat(false);
    assertEquals(403, get("/healthcheck").statusCode());
  }

  @Test
  void theDefaultsAnswerLocalCallersAndCheckThePortalContainer() throws Exception {
    // The defaults apply when the shell exports nothing: the test then says nothing
    assumeTrue(System.getenv("EXO_HEALTHCHECK_ALLOW") == null && System.getenv("EXO_HEALTHCHECK_CONTAINER") == null
        && System.getenv("EXO_HEALTHCHECK") == null);
    valve = new HealthCheckValve();
    System.setProperty("exo.healthcheck.deep", "false");
    startTomcat(false);
    assertEquals(503, get("/healthcheck").statusCode());
    register(true);
    assertEquals(200, get("/healthcheck").statusCode());
  }

  @Test
  void listsEveryProblem() throws Exception {
    valve.setPortalContainer("portal");
    startTomcat(true);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("webapp '/broken' is STOPPED"), response.body());
    assertTrue(response.body().contains("\",\"portal container 'portal' is not registered"), response.body());
  }

  @Test
  void isDownWhenThePortalContainerStateIsUnreadable() throws Exception {
    valve.setPortalContainer("portal");
    ManagementFactory.getPlatformMBeanServer()
                     .registerMBean(new javax.management.StandardMBean(new Other(), OtherMBean.class), PORTAL);
    startTomcat(false);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("portal container 'portal' state is unreadable"), response.body());
  }

  @Test
  void namesTheRootWebApplication() throws Exception {
    brokenRoot = true;
    startTomcat(false);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("webapp '/' is STOPPED"), response.body());
  }

  @Test
  void everyMatchingPortalContainerMustBeStarted() throws Exception {
    valve.setPortalContainer("portal");
    startTomcat(false);
    register(true);
    ObjectName other = name("exo:container=portal,name=portal,instance=second");
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    server.registerMBean(new javax.management.StandardMBean(new Container(false), ContainerMBean.class), other);
    try {
      HttpResponse<String> response = get("/healthcheck");
      assertEquals(503, response.statusCode());
      assertTrue(response.body().contains("portal container 'portal' is not started"), response.body());
    } finally {
      server.unregisterMBean(other);
    }
    assertEquals(200, get("/healthcheck").statusCode());
  }

  @Test
  void checksTheSearchServerOnceTheWebApplicationsAreStarted() throws Exception {
    com.sun.net.httpserver.HttpServer search = fakeSearch("{\"status\":\"green\"}");
    try {
      valve.setDeep("search");
      startTomcat(false);
      assertEquals(200, get("/healthcheck").statusCode());
      searchBody = "{\"status\":\"red\"}";
      HttpResponse<String> response = get("/healthcheck");
      assertEquals(503, response.statusCode());
      assertTrue(response.body().contains("search server cluster status is red"), response.body());
    } finally {
      search.stop(0);
    }
  }

  @Test
  void doesNotAskTheDependenciesWhileTheWebApplicationsAreNotStarted() throws Exception {
    com.sun.net.httpserver.HttpServer search = fakeSearch("{\"status\":\"red\"}");
    try {
      valve.setDeep("search");
      startTomcat(true);
      HttpResponse<String> response = get("/healthcheck");
      assertEquals(503, response.statusCode());
      assertTrue(response.body().contains("webapp '/broken'"), response.body());
      assertFalse(response.body().contains("search"), response.body());
      assertEquals(0, searchCalls.get());
    } finally {
      search.stop(0);
    }
  }

  @Test
  void keepsTheDependenciesAnswerForTheCacheDuration() throws Exception {
    com.sun.net.httpserver.HttpServer search = fakeSearch("{\"status\":\"green\"}");
    try {
      valve.setDeep("search");
      valve.setCacheSeconds(60);
      startTomcat(false);
      for (int i = 0; i < 3; i++) {
        assertEquals(200, get("/healthcheck").statusCode());
      }
      assertEquals(1, searchCalls.get());
    } finally {
      search.stop(0);
    }
  }

  @Test
  void theDependenciesAreCheckedByDefault() throws Exception {
    assumeTrue(System.getenv("EXO_HEALTHCHECK_DEEP") == null);
    com.sun.net.httpserver.HttpServer search = fakeSearch("{\"status\":\"green\"}");
    try {
      valve = new HealthCheckValve();
      valve.setAllow("127.0.0.1,::1");
      valve.setPortalContainer("none");
      valve.setCacheSeconds(0);
      startTomcat(false);
      assertEquals(200, get("/healthcheck").statusCode());
      assertEquals(1, searchCalls.get());
    } finally {
      search.stop(0);
    }
  }

  @Test
  void theDependenciesComeFromTheSystemProperty() throws Exception {
    valve = new HealthCheckValve();
    valve.setAllow("127.0.0.1,::1");
    valve.setPortalContainer("none");
    System.setProperty("exo.healthcheck.deep", "database");
    startTomcat(false);
    // No search server is called, and the server has no data source
    assertEquals(200, get("/healthcheck").statusCode());
  }

  @Test
  void checksTheDataSourcesOfTheRealNamingContextOfTomcat() throws Exception {
    valve.setDeep("database");
    withDataSource = true;
    DATA_SOURCE_MODE.set("valid");
    startTomcat(false);
    assertEquals(200, get("/healthcheck").statusCode());
    DATA_SOURCE_MODE.set("invalid");
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("database 'exo-test_portal' gives an invalid connection"), response.body());
    DATA_SOURCE_MODE.set("down");
    assertTrue(get("/healthcheck").body().contains("database 'exo-test_portal' is unreachable"));
  }

  @Test
  void isDownWhenNotInAHost() throws Exception {
    inEngine = true;
    startTomcat(false);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("not attached to a host"), response.body());
  }

  @Test
  void canBeDisabled() throws Exception {
    System.setProperty("exo.healthcheck", "false");
    startTomcat(false);
    assertEquals(404, get("/healthcheck").statusCode());
  }

  @Test
  void isDownWhenAWebApplicationFailedToStart() throws Exception {
    startTomcat(true);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("\"status\":\"DOWN\""), response.body());
    assertTrue(response.body().contains("webapp '/broken' is STOPPED"), response.body());
    assertEquals(0, SESSIONS.get());
  }

  @Test
  void isDownUntilThePortalContainerIsStarted() throws Exception {
    valve.setPortalContainer("portal");
    startTomcat(false);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("portal container 'portal' is not registered"), response.body());

    register(false);
    response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertTrue(response.body().contains("portal container 'portal' is not started"), response.body());

    unregister();
    register(true);
    assertEquals(200, get("/healthcheck").statusCode());
  }

  @Test
  void anEmptyOrNonePortalContainerOnlyChecksTheWebApplications() throws Exception {
    valve.setPortalContainer("None");
    startTomcat(false);
    assertEquals(200, get("/healthcheck").statusCode());
  }

  @Test
  void rejectsAnInvalidPortalContainerName() throws Exception {
    valve.setPortalContainer("portal,*");
    startTomcat(false);
    HttpResponse<String> response = get("/healthcheck");
    assertEquals(503, response.statusCode());
    assertFalse(response.body().contains("registered"));
  }

  private void startTomcat(boolean withBrokenWebApp) throws Exception {
    File base = Files.createTempDirectory("healthcheck").toFile();
    base.deleteOnExit();
    tomcat = new Tomcat();
    tomcat.setBaseDir(base.getAbsolutePath());
    tomcat.getConnector().setPort(0);
    tomcat.getConnector().setProperty("address", "127.0.0.1");
    tomcat.getHost().setAppBase(base.getAbsolutePath());
    (inEngine ? tomcat.getEngine() : tomcat.getHost()).getPipeline().addValve(valve);
    Context app = tomcat.addContext("/app", base.getAbsolutePath());
    Tomcat.addServlet(app, "page", new HttpServlet() {
      @Override
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.getSession(true);
        SESSIONS.incrementAndGet();
        resp.getWriter().write("app");
      }
    });
    app.addServletMappingDecoded("/page", "page");
    if (brokenRoot) {
      Context root = tomcat.addContext("", base.getAbsolutePath());
      root.addApplicationListener(FailingListener.class.getName());
    }
    if (withBrokenWebApp) {
      Context broken = tomcat.addContext("/broken", base.getAbsolutePath());
      broken.addApplicationListener(FailingListener.class.getName());
    }
    if (withDataSource) {
      tomcat.enableNaming();
      if (tomcat.getServer().getGlobalNamingResources() == null) {
        tomcat.getServer().setGlobalNamingResources(new org.apache.catalina.deploy.NamingResourcesImpl());
      }
      org.apache.tomcat.util.descriptor.web.ContextResource resource = new org.apache.tomcat.util.descriptor.web.ContextResource();
      resource.setName("exo-test_portal");
      resource.setType("javax.sql.DataSource");
      resource.setAuth("Container");
      resource.setProperty("factory", FakeDataSourceFactory.class.getName());
      tomcat.getServer().getGlobalNamingResources().addResource(resource);
    }
    tomcat.start();
  }

  private com.sun.net.httpserver.HttpServer fakeSearch(String body) throws IOException {
    searchBody = body;
    com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      searchCalls.incrementAndGet();
      byte[] bytes = searchBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    System.setProperty("exo.es.search.server.url", "http://127.0.0.1:" + server.getAddress().getPort());
    return server;
  }

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + path);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return send("GET", path);
  }

  private HttpResponse<String> send(String method, String path) throws Exception {
    return client.send(HttpRequest.newBuilder(uri(path)).method(method, HttpRequest.BodyPublishers.noBody()).build(),
                       BodyHandlers.ofString());
  }

  private static void register(boolean started) throws Exception {
    ManagementFactory.getPlatformMBeanServer().registerMBean(new javax.management.StandardMBean(new Container(started),
                                                                                                 ContainerMBean.class),
                                                             PORTAL);
  }

  private static void unregister() throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    if (server.isRegistered(PORTAL)) {
      server.unregisterMBean(PORTAL);
    }
  }

  private static ObjectName name(String name) {
    try {
      return new ObjectName(name);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public interface ContainerMBean {
    boolean isStarted();
  }

  public static class Container implements ContainerMBean {
    private final boolean started;

    Container(boolean started) {
      this.started = started;
    }

    @Override
    public boolean isStarted() {
      return started;
    }
  }

  /** Creates the data source of the global naming resources of Tomcat. */
  public static class FakeDataSourceFactory implements ObjectFactory {
    @Override
    public Object getObjectInstance(Object obj, Name name, javax.naming.Context nameCtx, Hashtable<?, ?> environment) {
      return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                                                      new Class<?>[] { javax.sql.DataSource.class },
                                                      (proxy, method, args) -> {
                                                        if ("down".equals(DATA_SOURCE_MODE.get())) {
                                                          throw new java.sql.SQLException("Connection refused");
                                                        }
                                                        boolean valid = "valid".equals(DATA_SOURCE_MODE.get());
                                                        return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                                                                                                        new Class<?>[] { java.sql.Connection.class },
                                                                                                        (p, m, a) -> "isValid".equals(m.getName()) ? Boolean.valueOf(valid) : null);
                                                      });
    }
  }

  public interface OtherMBean {
    boolean isReady();
  }

  public static class Other implements OtherMBean {
    @Override
    public boolean isReady() {
      return true;
    }
  }

  public static class FailingListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
      throw new IllegalStateException("The web application can't start");
    }
  }
}
