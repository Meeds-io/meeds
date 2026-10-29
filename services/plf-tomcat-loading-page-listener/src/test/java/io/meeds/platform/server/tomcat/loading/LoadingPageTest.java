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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadingPageTest {

  @TempDir
  Path                confDir;

  private LoadingPageServer server;

  private HttpClient        client = HttpClient.newHttpClient();

  @BeforeEach
  void start() throws IOException {
    server = new LoadingPageServer(java.net.InetAddress.getLoopbackAddress(), 0, new LoadingPageContent(confDir));
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop();
  }

  @Test
  void bundlesOfTheClasspathAreFoundWithoutAList() {
    List<String> tags = LoadingPageContent.AVAILABLE.stream().map(Locale::toLanguageTag).toList();
    assertEquals("en", tags.get(0), "The default language comes first");
    assertTrue(tags.containsAll(List.of("fr", "ar", "es-ES")), tags.toString());
  }

  @Test
  void translationsAddedToAJarAreFound() throws Exception {
    // Jar with a folder entry, and jar without it but with the default bundle
    Path withFolder = jarOf("with-folder.jar", true, "pt_BR", "ja");
    Path withoutFolder = jarOf("without-folder.jar", false, "en", "ko");
    try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] { withFolder.toUri().toURL(), withoutFolder.toUri().toURL() },
                                                                      LoadingPageContent.class.getClassLoader())) {
      List<String> tags = LoadingPageContent.findAvailableLocales(loader).stream().map(Locale::toLanguageTag).toList();
      assertTrue(tags.containsAll(List.of("en", "fr", "pt-BR", "ja", "ko")), tags.toString());
      assertEquals("en", tags.get(0));
    }
  }

  private Path jarOf(String name, boolean folderEntry, String... tags) throws IOException {
    Path jar = confDir.resolve(name);
    try (java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(Files.newOutputStream(jar))) {
      if (folderEntry) {
        out.putNextEntry(new java.util.jar.JarEntry("locale/loading/"));
        out.closeEntry();
      }
      for (String tag : tags) {
        out.putNextEntry(new java.util.jar.JarEntry("locale/loading/loading_" + tag + ".properties"));
        out.write("loading.title=Test\n".getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
      }
    }
    return jar;
  }

  @Test
  void resolveLocaleFromAcceptLanguage() {
    assertEquals("fr", LoadingPageContent.resolveLocale("fr-FR,fr;q=0.9,en;q=0.8", null).getLanguage());
    assertEquals("ar", LoadingPageContent.resolveLocale("ar-EG,ar;q=0.9", null).getLanguage());
    // any region of a language uses the bundle available for it
    assertEquals(Locale.forLanguageTag("es-ES"), LoadingPageContent.resolveLocale("es-MX,es;q=0.9", null));
  }

  @Test
  void resolveLocaleUsesQualityAndFallsBackToTheDefaultLanguage() {
    assertEquals("ar", LoadingPageContent.resolveLocale("fr;q=0.4, ar;q=0.8", null).getLanguage());
    assertEquals("fr", LoadingPageContent.resolveLocale("ja,zh;q=0.9,fr;q=0.5", null).getLanguage());
    assertEquals("en", LoadingPageContent.resolveLocale("ja,zh;q=0.9", null).getLanguage());
    assertEquals("en", LoadingPageContent.resolveLocale("not a header;;;", null).getLanguage());
    assertEquals("en", LoadingPageContent.resolveLocale(null, null).getLanguage());
  }

  @Test
  void langParameterWinsOverHeader() {
    assertEquals("ar", LoadingPageContent.resolveLocale("fr", "ar").getLanguage());
    assertEquals("fr", LoadingPageContent.resolveLocale("fr", "xx").getLanguage());
  }

  @Test
  void aTranslationMissingSomeTextsFallsBackToTheDefaultLanguage() {
    LoadingPageContent content = new LoadingPageContent(confDir);
    String english = content.page(Locale.forLanguageTag("en"));
    for (Locale locale : LoadingPageContent.AVAILABLE) {
      assertFalse(content.page(locale).contains("{{"), "Unresolved placeholder for " + locale);
    }
    String french = content.page(Locale.forLanguageTag("fr"));
    assertTrue(french.contains("Nous préparons votre espace de travail"));
    assertTrue(french.contains("Please wait, you will be redirected automatically."), "Missing text uses the default language");
    assertNotEquals(english, french);
  }

  @Test
  void rightToLeftLanguagesGetTheRightDirection() {
    LoadingPageContent content = new LoadingPageContent(confDir);
    assertTrue(content.page(Locale.forLanguageTag("ar")).contains("dir=\"rtl\""));
    assertTrue(content.page(Locale.forLanguageTag("fr")).contains("dir=\"ltr\""));
    assertTrue(content.page(Locale.forLanguageTag("ar")).contains("جارٍ تشغيل الخادم"));
  }

  @Test
  void servesTheLocalizedPageWithServiceUnavailable() throws Exception {
    HttpResponse<String> response = get("/portal/dw", "fr-FR,fr;q=0.9");
    assertEquals(503, response.statusCode());
    assertEquals("10", response.headers().firstValue("Retry-After").orElse(null));
    assertEquals("true", response.headers().firstValue(LoadingPageServer.MARKER_HEADER).orElse(null));
    assertEquals("fr", response.headers().firstValue("Content-Language").orElse(null));
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(null));
    assertTrue(response.body().contains("Nous préparons votre espace de travail"));
  }

  @Test
  void pageRespectsItsOwnContentSecurityPolicy() throws Exception {
    HttpResponse<String> response = get("/", null);
    String policy = response.headers().firstValue("Content-Security-Policy").orElse("");
    // the ticks of the checklist are an inline image in the stylesheet
    assertTrue(policy.contains("img-src 'self' data:"), policy);
    assertFalse(policy.contains("unsafe-inline"), policy);
    // so no inline style or script may be needed: the browser would refuse them
    assertFalse(response.body().contains(" style=\""), "Inline style attribute");
    assertFalse(response.body().matches("(?s).*<script(?![^>]*src=)[^>]*>.*"), "Inline script");
    assertFalse(response.body().matches("(?s).*<style[ >].*"), "Inline style element");
    assertTrue(get("/_loading/loading.css", null).body().contains("data:image/svg+xml"));
  }

  @Test
  void langQueryParameterSelectsTheLanguage() throws Exception {
    assertEquals("fr", get("/?lang=fr", "ar").headers().firstValue("Content-Language").orElse(null));
    assertEquals("ar", get("/portal?x=1&lang=ar", null).headers().firstValue("Content-Language").orElse(null));
    // a parameter without value is ignored: the language of the browser is used
    assertEquals("fr", get("/?lang", "fr").headers().firstValue("Content-Language").orElse(null));
    assertEquals("fr", get("/?lang=xx", "fr").headers().firstValue("Content-Language").orElse(null));
  }

  @Test
  void canListenOnAllInterfaces() throws Exception {
    LoadingPageServer all = new LoadingPageServer(null, 0, new LoadingPageContent(confDir));
    all.start();
    try {
      HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + all.getPort() + "/")).build(),
                                                  BodyHandlers.ofString());
      assertEquals(503, response.statusCode());
    } finally {
      all.stop();
    }
  }

  @Test
  void escapesTheCharactersOfHtml() {
    assertEquals("&lt;a href=&quot;x&quot;&gt;&amp;&#39;", LoadingPageContent.escape("<a href=\"x\">&'"));
  }

  @Test
  void worksWithoutAConfigurationFolderAndWithBrokenBranding() throws Exception {
    LoadingPageContent content = new LoadingPageContent(null);
    assertTrue(content.page(Locale.forLanguageTag("en")).contains("Meeds"));
    assertTrue(content.asset("logo.png").bytes().length > 0);
    assertEquals("image/png", content.asset("logo.png").contentType());

    Files.writeString(confDir.resolve("branding.properties"), "color.primary=#123456\nbroken=\\uZZZZ\n");
    LoadingPageContent broken = new LoadingPageContent(confDir);
    assertTrue(broken.page(Locale.forLanguageTag("en")).contains("Meeds"), "Defaults are used when the branding can't be read");
  }

  @Test
  void anOverrideThatCanNotBeReadIsIgnored() throws Exception {
    Path logo = confDir.resolve("logo.png");
    Files.writeString(logo, "custom");
    Files.setPosixFilePermissions(logo, java.util.Set.of());
    org.junit.jupiter.api.Assumptions.assumeFalse(Files.isReadable(logo), "Permissions are not enforced for this user");
    byte[] custom = new LoadingPageContent(confDir).asset("logo.png").bytes();
    assertNotEquals("custom", new String(custom, StandardCharsets.UTF_8));
    assertTrue(custom.length > 6);
  }

  @Test
  void aCheckpointWithoutLabelShowsItsId() throws Exception {
    Files.writeString(confDir.resolve("checkpoints.conf"), "unlabelled=Some text\n");
    assertTrue(new LoadingPageContent(confDir).page(Locale.forLanguageTag("en")).contains("<li data-id=\"unlabelled\">unlabelled</li>"));
  }

  @Test
  void aLanguageWithoutBundleUsesTheDefaultTexts() {
    assertTrue(new LoadingPageContent(confDir).page(Locale.forLanguageTag("ja")).contains("We are preparing your workplace"));
  }

  @Test
  void resourcesThatCanNotBeLoaded() {
    ClassLoader own = LoadingPageContent.class.getClassLoader();
    assertThrows(IllegalStateException.class, () -> LoadingPageContent.classpathBytes(own, "loading/nothing"));
    ClassLoader failing = new ClassLoader(null) {
      @Override
      public java.io.InputStream getResourceAsStream(String name) {
        return new java.io.InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("broken");
          }
        };
      }
    };
    assertThrows(IllegalStateException.class, () -> LoadingPageContent.classpathBytes(failing, "loading/index.html"));
    ClassLoader unsearchable = new ClassLoader(null) {
      @Override
      public java.util.Enumeration<java.net.URL> getResources(String name) throws IOException {
        throw new IOException("broken");
      }
    };
    assertEquals(List.of(Locale.forLanguageTag("en")), LoadingPageContent.findAvailableLocales(unsearchable));
  }

  @Test
  void glassStyleCanBeTunedAndHasItsFallbacks() throws Exception {
    String css = get("/_loading/loading.css", null).body();
    // the opacity and the blur of the glass are variables a distribution can change
    assertTrue(css.contains("--glass:") && css.contains("--blur:"), "Tunable glass");
    assertTrue(css.contains("backdrop-filter:blur(var(--blur))"));
    // solid surfaces when there is no blur, or when less transparency is requested
    assertTrue(css.contains("@supports not ((backdrop-filter:blur(1px))"));
    assertTrue(css.contains("prefers-reduced-transparency:reduce"));
    assertTrue(get("/_loading/custom.css", null).body().contains("--glass"), "Documented for the distributions");
  }

  @Test
  void headRequestHasNoBody() throws Exception {
    HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/")).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                                                BodyHandlers.ofString());
    assertEquals(503, response.statusCode());
    assertEquals("true", response.headers().firstValue(LoadingPageServer.MARKER_HEADER).orElse(null));
    assertEquals("", response.body());
  }

  @Test
  void servesAssetsAndOnlyKnownOnes() throws Exception {
    assertEquals(200, get("/_loading/loading.css", null).statusCode());
    assertEquals(200, get("/_loading/loading.js", null).statusCode());
    assertEquals(200, get("/_loading/logo.png", null).statusCode());
    assertEquals(200, get("/_loading/logo-dark.png", null).statusCode());
    assertEquals(404, get("/_loading/branding.properties", null).statusCode());
    assertEquals(404, get("/_loading/..%2f..%2fetc%2fpasswd", null).statusCode());
  }

  @Test
  void logoAndColorsCanBeOverriddenByTheDistribution() throws Exception {
    Files.write(confDir.resolve("logo.png"), "custom-logo".getBytes(StandardCharsets.UTF_8));
    Files.writeString(confDir.resolve("branding.properties"),
                      "product.name=eXo <Platform>\ncolor.primary=#123456\ncolor.secondary=red;}body{display:none\n");
    server.stop();
    start();
    assertEquals("custom-logo", get("/_loading/logo.png", null).body());
    // dark logo falls back to the distribution logo
    assertEquals("custom-logo", get("/_loading/logo-dark.png", null).body());
    String theme = get("/_loading/theme.css", null).body();
    assertTrue(theme.contains("--primary:#123456"));
    assertFalse(theme.contains("display:none"), "Invalid color must be ignored");
    assertTrue(theme.contains("--secondary:#e25d5d"));
    String page = get("/", null).body();
    assertTrue(page.contains("eXo &lt;Platform&gt;"));
    assertFalse(page.contains("eXo <Platform>"));
  }

  @Test
  void reportsTheStatusOfTheServer() throws Exception {
    HttpResponse<String> first = get("/_loading/status", null);
    assertEquals(200, first.statusCode());
    assertEquals("no-store", first.headers().firstValue("Cache-Control").orElse(null));
    assertEquals("true", first.headers().firstValue(LoadingPageServer.MARKER_HEADER).orElse(null));
    assertTrue(first.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
    assertTrue(first.body().contains("\"phase\":\"starting\""), first.body());
    long elapsed = elapsedOf(first.body());
    long limit = System.nanoTime() + 5_000_000_000L;
    long later = elapsed;
    while (later < elapsed + 50 && System.nanoTime() < limit) {
      later = elapsedOf(get("/_loading/status", null).body());
    }
    assertTrue(later >= elapsed + 50, "Elapsed time must keep growing on the server");
  }

  @Test
  void statusFollowsTheProgress() throws Exception {
    StartupProgress progress = new StartupProgress();
    server.stop();
    server = new LoadingPageServer(java.net.InetAddress.getLoopbackAddress(), 0, new LoadingPageContent(confDir), progress);
    server.start();
    progress.setTotal(2);
    progress.applicationSeen();
    progress.applicationStarted();
    String body = get("/_loading/status", null).body();
    assertTrue(body.contains("\"phase\":\"deploying\"") && body.contains("\"deployed\":1") && body.contains("\"total\":2"), body);
  }

  private static long elapsedOf(String json) {
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"elapsed\":(\\d+)").matcher(json);
    assertTrue(matcher.find(), json);
    return Long.parseLong(matcher.group(1));
  }

  @Test
  void listenerReportsAFailedStartupBeforeClosing() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    LoadingPageListener listener = new LoadingPageListener();
    listener.start(java.net.InetAddress.getLoopbackAddress(), port, null);
    // The server is destroyed without the connector having been started
    listener.stopAfterFailure();
    HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/_loading/status")).build(),
                                                BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"phase\":\"failed\""), response.body());
    listener.stop();
  }

  @Test
  void customStyleIsLoadedLastAndCanBeReplacedByTheDistribution() throws Exception {
    String page = get("/", null).body();
    assertTrue(page.indexOf("/_loading/loading.css") < page.indexOf("/_loading/theme.css"));
    assertTrue(page.indexOf("/_loading/theme.css") < page.indexOf("/_loading/custom.css"), "custom.css must come last");

    HttpResponse<String> defaultStyle = get("/_loading/custom.css", null);
    assertEquals(200, defaultStyle.statusCode());
    assertTrue(defaultStyle.headers().firstValue("Content-Type").orElse("").startsWith("text/css"));
    assertFalse(defaultStyle.body().replaceAll("(?s)/\\*.*?\\*/", "").contains("{"), "No rule by default");

    Files.writeString(confDir.resolve("custom.css"), ":root{--bg:#101010}\n@media (prefers-color-scheme:dark){:root{--bg:#000}}\n");
    assertTrue(get("/_loading/custom.css", null).body().contains("--bg:#101010"));
  }

  @Test
  void releasesThePortOnStop() throws Exception {
    int port = server.getPort();
    server.stop();
    try (ServerSocket socket = new ServerSocket(port)) {
      assertNotNull(socket);
      assertEquals(port, socket.getLocalPort());
    }
  }

  @Test
  void listenerStartsAndStops() throws Exception {
    LoadingPageListener listener = new LoadingPageListener();
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    listener.start(java.net.InetAddress.getLoopbackAddress(), port, null);
    HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).build(),
                                                BodyHandlers.ofString());
    assertEquals(503, response.statusCode());
    listener.stop();
    try (ServerSocket socket = new ServerSocket(port)) {
      assertEquals(port, socket.getLocalPort());
    }
  }

  @Test
  void listenerFollowsTheLifecycleOfAConnector() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setPort(port);
    connector.setProperty("address", "127.0.0.1");
    LoadingPageListener listener = new LoadingPageListener();
    listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.BEFORE_INIT_EVENT, null));
    HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).build(),
                                                BodyHandlers.ofString());
    assertEquals(503, response.statusCode());
    // The port is released when the connector is about to bind it
    listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.BEFORE_START_EVENT, null));
    try (ServerSocket socket = new ServerSocket(port)) {
      assertEquals(port, socket.getLocalPort());
    }
  }

  @Test
  void listenerDoesNotFailWhenThePortIsUsed() throws Exception {
    try (ServerSocket socket = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      LoadingPageListener listener = new LoadingPageListener();
      listener.start(java.net.InetAddress.getLoopbackAddress(), socket.getLocalPort(), null);
      listener.stop();
      assertNull(null);
    }
  }

  private HttpResponse<String> get(String path, String acceptLanguage) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(uri(path));
    if (acceptLanguage != null) {
      request.header("Accept-Language", acceptLanguage);
    }
    return client.send(request.build(), BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + server.getPort() + path);
  }

}
