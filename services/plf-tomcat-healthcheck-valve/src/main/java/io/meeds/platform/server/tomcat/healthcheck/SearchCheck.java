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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Checks that the Elasticsearch server of the platform answers and is not
 * <code>red</code>. The server is the one the platform uses, read from the
 * system properties it writes when it loads its configuration
 * (<code>exo.es.search.server.url</code>, <code>.username</code> and
 * <code>.password</code>). Nothing of the request reaches the call, and
 * neither the address nor the credentials are written in the answer nor in
 * the logs.
 */
final class SearchCheck {

  /** Logger of the check. */
  private static final Log      LOG         = LogFactory.getLog(SearchCheck.class);

  /** Address used by the platform when none is configured. */
  private static final String   DEFAULT_URL = "http://127.0.0.1:9200";

  /**
   * Shared by the checks: a client owns a thread and its connections. It never
   * goes through the proxy of the JVM, like the client of the platform: the
   * credentials must reach the search server only. A request is given its own
   * timeout, which only bounds the wait for the headers of the answer: what
   * bounds the caller is the deadline of {@link DependencyHealth}.
   */
  private static final HttpClient CLIENT   = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

  /** Status of the cluster in the answer of the health API. */
  private static final Pattern  STATUS      = Pattern.compile("\"status\"\\s*:\\s*\"(\\w+)\"");

  /** First status code that is not a successful answer (redirections are not followed). */
  private static final int      STATUS_REDIRECT = 300;

  /** Status code of an unknown user. */
  private static final int      UNAUTHORIZED    = 401;

  /** Status code of a user that may not read the health. */
  private static final int      FORBIDDEN       = 403;

  private SearchCheck() {
    // Utility class
  }

  /**
   * @param timeoutSeconds seconds to get the headers of the answer, the
   *                       connection included
   * @return null when the server answers and is green or yellow, the reason
   *         otherwise
   */
  static String check(int timeoutSeconds) {
    URI uri = healthUri();
    if (uri == null) {
      return "has an unsupported address";
    }
    try {
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(timeoutSeconds)).GET();
      String user = System.getProperty("exo.es.search.server.username");
      if (user != null && !user.isEmpty()) {
        String credentials = user + ":" + System.getProperty("exo.es.search.server.password", "");
        request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
      }
      HttpResponse<String> response = CLIENT.send(request.build(), BodyHandlers.ofString());
      return reason(response.statusCode(), response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return "did not answer in time";
    } catch (Exception e) {
      LOG.warn("The search server does not answer: " + e.getClass().getSimpleName());
      return "is unreachable";
    }
  }

  private static String reason(int status, String body) {
    if (status == UNAUTHORIZED || status == FORBIDDEN) {
      return "refuses the user (it needs the monitor privilege)";
    }
    if (status >= STATUS_REDIRECT) {
      return "answered HTTP " + status;
    }
    Matcher matcher = STATUS.matcher(body);
    if (!matcher.find()) {
      return "answered without a status";
    }
    // The status is the one of the whole cluster, not only of the indices of the platform
    return "red".equals(matcher.group(1)) ? "cluster status is red" : null;
  }

  /**
   * @return the address of the health API, null when the configured address is
   *         not an HTTP or HTTPS one
   */
  private static URI healthUri() {
    String url = System.getProperty("exo.es.search.server.url", DEFAULT_URL).strip();
    if (url.isEmpty()) {
      // The platform treats a blank address as unset
      url = DEFAULT_URL;
    }
    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      return null;
    }
    try {
      return URI.create(url + "/_cluster/health");
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
