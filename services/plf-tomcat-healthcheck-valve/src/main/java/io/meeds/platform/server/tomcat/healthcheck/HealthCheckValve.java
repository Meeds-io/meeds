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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import jakarta.servlet.ServletException;

/**
 * A {@link org.apache.catalina.Valve} of the Host answering the health check
 * of the platform. It runs before any web application: no filter, no
 * authentication and no <code>HttpSession</code> is involved.
 * <ul>
 * <li><code>200</code> when every web application is started, the portal
 * container is started and, unless {@link #setDeep(String)} says otherwise,
 * what the platform depends on answers: every JDBC pool declared in the
 * global naming resources of the server gives a valid connection, and the
 * Elasticsearch server of the platform answers and is not red. Each is given a
 * few seconds and the result is kept for {@link #setCacheSeconds(int)}</li>
 * <li><code>503</code> with the reasons otherwise</li>
 * <li><code>403</code> when the caller is not allowed, see
 * {@link #setAllow(String)}, or when the request carries a header set by a
 * reverse proxy (<code>X-Forwarded-For</code>, <code>Forwarded</code>,
 * <code>X-Real-IP</code>, <code>X-Forwarded-Proto</code>,
 * <code>X-Forwarded-Host</code>, <code>X-Forwarded-By</code> or
 * <code>Via</code>): a proxy running on the same machine would otherwise
 * make every caller look local. A reverse proxy that forwards
 * <code>/healthcheck</code> without any of them still makes the caller look
 * local: block that path in the proxy, or disable the health check</li>
 * <li><code>405</code> for any method but <code>GET</code> and
 * <code>HEAD</code></li>
 * </ul>
 * Declare it in the <code>Host</code>, before the <code>RemoteIpValve</code>
 * so that the address is the one of the TCP peer.
 * <p>
 * Set <code>EXO_HEALTHCHECK=false</code> (or the system property
 * <code>exo.healthcheck</code>) to disable it.
 */
public final class HealthCheckValve extends ValveBase {

  /** Logger of the valve. */
  private static final Log   LOG            = LogFactory.getLog(HealthCheckValve.class);

  /** Allowed callers when nothing is configured. */
  private static final String DEFAULT_ALLOW = "127.0.0.1,::1";

  /** Seconds the result of the dependencies is kept by default. */
  private static final int DEFAULT_CACHE_SECONDS = 10;

  /** Status of a healthy platform. */
  private static final int STATUS_OK = 200;

  /** Status when the caller is not allowed. */
  private static final int STATUS_FORBIDDEN = 403;

  /** Status when the method is not GET nor HEAD. */
  private static final int STATUS_METHOD_NOT_ALLOWED = 405;

  /** Status of an unhealthy platform. */
  private static final int STATUS_UNAVAILABLE = 503;

  /** Headers set by a reverse proxy. */
  private static final String[] PROXY_HEADERS = {"X-Forwarded-For", "Forwarded", "X-Real-IP", "X-Forwarded-Proto",
                                                  "X-Forwarded-Host", "X-Forwarded-By", "Via"};

  /** URI answered by the valve. */
  private String             path           = "/healthcheck"; // NOSONAR default URI, set with setPath

  /** Configured allowed callers, null for the default. */
  private String             allow;

  /** Configured portal container, null for the default. */
  private String             portalContainer;

  /** Parsed allowed callers. */
  private AddressAllowList   allowList;

  /** Reader of the platform state. */
  private PlatformHealth     health;

  /** Configured dependencies to check, null for the default. */
  private String             deep;

  /** Seconds the result of the dependencies is kept. */
  private int                cacheSeconds   = DEFAULT_CACHE_SECONDS;

  /** Checks of the databases and the search server. */
  private DependencyHealth   dependencies;

  public HealthCheckValve() {
    super(true);
  }

  /**
   * @param uri URI answered by the valve, <code>/healthcheck</code> by default
   */
  public void setPath(String uri) {
    this.path = uri;
  }

  /**
   * @param callers comma separated IP addresses or CIDR networks allowed to
   *                call the health check, defaults to the property
   *                <code>exo.healthcheck.allow</code> or the variable
   *                <code>EXO_HEALTHCHECK_ALLOW</code>, then to
   *                <code>127.0.0.1,::1</code>
   */
  public void setAllow(String callers) {
    this.allow = callers;
  }

  /**
   * @param containerName name of the portal container that must be started,
   *                      <code>none</code> to skip the portal container check
   *                      (the web applications, and unless
   *                      {@link #setDeep(String)} says otherwise the database
   *                      and the search server, are still checked), defaults
   *                      to the property
   *                      <code>exo.healthcheck.container</code> or the variable
   *                      <code>EXO_HEALTHCHECK_CONTAINER</code>, then to
   *                      <code>portal</code>
   */
  public void setPortalContainer(String containerName) {
    this.portalContainer = containerName;
  }

  /**
   * @param checks the dependencies checked once the platform is started:
   *               <code>true</code> for the databases and the search server,
   *               <code>false</code> or <code>none</code> for none, or a comma
   *               separated list of <code>database</code> and
   *               <code>search</code>. Defaults to the property
   *               <code>exo.healthcheck.deep</code> or the variable
   *               <code>EXO_HEALTHCHECK_DEEP</code>, then to <code>true</code>
   */
  public void setDeep(String checks) {
    this.deep = checks;
  }

  /**
   * @param seconds seconds the result of the dependencies is kept, 10 by
   *                default: however often it is called, the health check asks
   *                the dependencies once in that time
   */
  public void setCacheSeconds(int seconds) {
    this.cacheSeconds = seconds;
  }

  @Override
  protected void initInternal() throws LifecycleException {
    super.initInternal();
    String callers = setting(allow, "exo.healthcheck.allow", "EXO_HEALTHCHECK_ALLOW", DEFAULT_ALLOW);
    try {
      allowList = new AddressAllowList(callers);
    } catch (IllegalArgumentException e) {
      // A typo must not stop the platform, the health check then only answers the local machine
      LOG.error("Invalid health check allow list '" + callers + "', only " + DEFAULT_ALLOW + " are allowed: " + e.getMessage());
      allowList = new AddressAllowList(DEFAULT_ALLOW);
    }
    health = new PlatformHealth(setting(portalContainer, "exo.healthcheck.container", "EXO_HEALTHCHECK_CONTAINER", "portal"));
  }

  @Override
  protected void startInternal() throws LifecycleException {
    super.startInternal();
    // Created at each start: stopInternal closes it, and a stopped host may be started again
    dependencies = new DependencyHealth(DependencyHealth.parse(setting(deep, "exo.healthcheck.deep", "EXO_HEALTHCHECK_DEEP", "true")),
                                        cacheSeconds);
  }

  @Override
  protected void stopInternal() throws LifecycleException {
    super.stopInternal();
    if (dependencies != null) {
      dependencies.close();
    }
  }

  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {
    if (!isEnabled() || !path.equals(request.getRequestURI())) {
      getNext().invoke(request, response);
      return;
    }
    if (!isAllowed(request)) {
      write(response, STATUS_FORBIDDEN, "{\"status\":\"FORBIDDEN\"}", false);
      return;
    }
    String method = request.getMethod();
    boolean head = "HEAD".equals(method);
    if (!head && !"GET".equals(method)) {
      response.setHeader("Allow", "GET, HEAD");
      write(response, STATUS_METHOD_NOT_ALLOWED, "{\"status\":\"METHOD_NOT_ALLOWED\"}", false);
      return;
    }
    Container host = getContainer();
    List<String> problems = host instanceof Host h ? problems(h) : List.of("not attached to a host");
    if (problems.isEmpty()) {
      write(response, STATUS_OK, "{\"status\":\"UP\"}", head);
    } else {
      LOG.debug("Health check is down: " + problems);
      write(response, STATUS_UNAVAILABLE, "{\"status\":\"DOWN\",\"problems\":[" + quoted(problems) + "]}", head);
    }
  }

  /**
   * The platform must be started before what it depends on is asked.
   *
   * @param host the host of the web applications
   * @return the reasons why the platform is not healthy, empty when it is
   */
  private List<String> problems(Host host) {
    List<String> problems = health.problems(host);
    if (problems.isEmpty() && dependencies.isEnabled()) {
      Server server = ((Engine) host.getParent()).getService().getServer();
      return dependencies.problems(server);
    }
    return problems;
  }

  private boolean isAllowed(Request request) {
    for (String header : PROXY_HEADERS) {
      if (request.getHeader(header) != null) {
        return false;
      }
    }
    return allowList.allows(request.getRemoteAddr());
  }

  private static boolean isEnabled() {
    String flag = System.getProperty("exo.healthcheck", System.getenv("EXO_HEALTHCHECK"));
    return !(flag != null && (flag.equalsIgnoreCase("false") || flag.equals("0") || flag.equalsIgnoreCase("no")));
  }

  private static String setting(String value, String property, String variable, String fallback) {
    if (value != null) {
      return value;
    }
    String configured = System.getProperty(property, System.getenv(variable));
    // A blank value (an empty variable of a compose file) means the default
    return configured == null || configured.isBlank() ? fallback : configured;
  }

  /**
   * @param values texts to write in a JSON array
   * @return the texts quoted and separated by commas, with the quote, the
   *         backslash and every control character escaped
   */
  static String quoted(List<String> values) {
    StringBuilder json = new StringBuilder();
    for (String value : values) {
      if (!json.isEmpty()) {
        json.append(',');
      }
      json.append('"');
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (c == '"' || c == '\\') {
          json.append('\\').append(c);
        } else if (c < ' ') {
          json.append(String.format("\\u%04x", (int) c));
        } else {
          json.append(c);
        }
      }
      json.append('"');
    }
    return json.toString();
  }

  private static void write(Response response, int status, String body, boolean head) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    response.setContentLength(bytes.length);
    if (!head) {
      response.getOutputStream().write(bytes);
    }
  }
}
