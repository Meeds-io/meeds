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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A tiny HTTP server that answers every request with the loading page and a
 * 503 status until it is stopped. It only relies on the JDK.
 */
public final class LoadingPageServer {

  /** Path prefix of the static assets of the page. */
  public static final String         ASSETS_PATH   = "/_loading/"; // NOSONAR fixed path of the files of the page

  /** Nanoseconds in a millisecond. */
  private static final long          NANOS_PER_MILLI    = 1_000_000L;

  /** HTTP status of a served file. */
  private static final int           STATUS_OK          = 200;

  /** HTTP status of an unknown file. */
  private static final int           STATUS_NOT_FOUND   = 404;

  /** HTTP status of the loading page itself. */
  private static final int           STATUS_UNAVAILABLE = 503;

  /** Name of the resource returning the startup status as JSON. */
  public static final String         STATUS        = "status";

  /** Header telling the page that the response comes from this server. */
  public static final String         MARKER_HEADER = "X-Startup-Page";

  /** Content security policy: the page only loads its own files. */
  private static final String        CSP           = "default-src 'none'; img-src 'self' data:; style-src 'self'; script-src 'self'; "
      + "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";

  /** Content served by the server. */
  private final LoadingPageContent   content;

  /** Progress reported by the status resource. */
  private final StartupProgress      progress;

  /** Running loading page server, null when stopped. */
  private final HttpServer           server;

  /** Threads answering the requests. */
  private final ExecutorService      executor;

  /** Time the server started, in nanoseconds. */
  private volatile long              startedAt     = System.nanoTime();

  /**
   * Binds the server, it starts answering after {@link #start()}.
   *
   * @param  address     the address to bind, null for all interfaces
   * @param  port        the port to bind, 0 for any free port
   * @param  pageContent the content to serve
   * @throws IOException when the port can't be bound
   */
  public LoadingPageServer(InetAddress address, int port, LoadingPageContent pageContent) throws IOException {
    this(address, port, pageContent, new StartupProgress());
  }

  /**
   * Binds the server, it starts answering after {@link #start()}.
   *
   * @param  address     the address to bind, null for all interfaces
   * @param  port        the port to bind, 0 for any free port
   * @param  pageContent     the content to serve
   * @param  startupProgress the progress reported by the status resource
   * @throws IOException when the port can't be bound
   */
  public LoadingPageServer(InetAddress address, int port, LoadingPageContent pageContent, StartupProgress startupProgress) throws IOException {
    this.content = pageContent;
    this.progress = startupProgress;
    this.server = HttpServer.create(address == null ? new InetSocketAddress(port) : new InetSocketAddress(address, port), 0);
    this.executor = Executors.newFixedThreadPool(2, runnable -> {
      Thread thread = new Thread(runnable, "loading-page");
      thread.setDaemon(true);
      return thread;
    });
    this.server.setExecutor(executor);
    this.server.createContext("/", this::handle);
  }

  public void start() {
    startedAt = System.nanoTime();
    server.start();
  }

  /**
   * Stops answering and releases the port immediately.
   */
  public void stop() {
    server.stop(0);
    executor.shutdownNow();
  }

  public int getPort() {
    return server.getAddress().getPort();
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      String path = exchange.getRequestURI().getRawPath();
      if ((ASSETS_PATH + STATUS).equals(path)) {
        sendStatus(exchange);
      } else if (path != null && path.startsWith(ASSETS_PATH)) {
        sendAsset(exchange, URLDecoder.decode(path.substring(ASSETS_PATH.length()), StandardCharsets.UTF_8));
      } else {
        sendPage(exchange);
      }
    }
  }

  private void sendPage(HttpExchange exchange) throws IOException {
    Locale locale = LoadingPageContent.resolveLocale(exchange.getRequestHeaders().getFirst("Accept-Language"),
                                                     queryParameter(exchange.getRequestURI().getRawQuery(), "lang"));
    exchange.getResponseHeaders().set("Content-Language", locale.toLanguageTag());
    exchange.getResponseHeaders().set("Retry-After", "10");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.getResponseHeaders().set("Vary", "Accept-Language");
    send(exchange, STATUS_UNAVAILABLE, "text/html; charset=UTF-8", content.page(locale).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * The elapsed time and the progress are the ones of the server, so that they
   * are kept when the page is refreshed or opened in another tab.
   *
   * @param  exchange    the request to answer
   * @throws IOException when the response can't be written
   */
  private void sendStatus(HttpExchange exchange) throws IOException {
    long millis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI;
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    send(exchange, STATUS_OK, "application/json; charset=UTF-8", progress.toJson(millis).getBytes(StandardCharsets.UTF_8));
  }

  private void sendAsset(HttpExchange exchange, String name) throws IOException {
    LoadingPageContent.Asset asset = content.asset(name);
    if (asset == null) {
      send(exchange, STATUS_NOT_FOUND, "text/plain; charset=UTF-8", "Not found".getBytes(StandardCharsets.UTF_8));
    } else {
      exchange.getResponseHeaders().set("Cache-Control", "no-cache");
      send(exchange, STATUS_OK, asset.contentType(), asset.bytes());
    }
  }

  private void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    exchange.getResponseHeaders().set("Content-Security-Policy", CSP);
    exchange.getResponseHeaders().set(MARKER_HEADER, "true");
    boolean head = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
    exchange.sendResponseHeaders(status, head ? -1 : body.length);
    if (!head) {
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    }
  }

  private static String queryParameter(String query, String name) {
    if (query != null) {
      for (String pair : query.split("&")) {
        int index = pair.indexOf('=');
        if (index > 0 && pair.substring(0, index).equals(name)) {
          return URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
        }
      }
    }
    return null;
  }

}
