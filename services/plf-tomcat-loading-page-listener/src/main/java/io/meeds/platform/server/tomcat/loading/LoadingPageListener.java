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

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * A {@link LifecycleListener} to attach to the HTTP {@link Connector}. Tomcat
 * opens the port of the connector once every web application is deployed (the
 * connector must be configured with <code>bindOnInit="false"</code>), so until
 * then browsers get a connection error. This listener answers on that port
 * with a loading page and releases it right before the connector binds it.
 * <p>
 * Set the environment variable <code>EXO_LOADING_PAGE</code> or the system
 * property <code>exo.loading.page</code> to <code>false</code> to disable it.
 */
public final class LoadingPageListener implements LifecycleListener {

  /** Time the page keeps answering after the server decided to stop, in milliseconds by default. */
  private static final long    DEFAULT_FAILURE_GRACE_MILLIS = 8000;

  /** Logger of the listener. */
  private static final Log     LOG = LogFactory.getLog(LoadingPageListener.class);

  /** Whether the loading page can be served. */
  private boolean              enabled = true;

  /** Directory of the optional branding files. */
  private String               confDir;

  /** Running loading page server, null when stopped. */
  private LoadingPageServer    server;

  /** Follows the deployments of the hosts, null when stopped. */
  private DeploymentTracker    tracker;

  /** Time the page keeps answering after the server decided to stop, in milliseconds. */
  private long                 failureGraceMillis = DEFAULT_FAILURE_GRACE_MILLIS;

  /** Platform log file in which the checkpoints are looked for. */
  private String               logFile;

  /** Progress reported by the running server, null when stopped. */
  private StartupProgress      progress;

  /** Reader of the platform log, null when stopped. */
  private CheckpointTracker    logTracker;

  @Override
  public void lifecycleEvent(LifecycleEvent event) {
    String type = event.getType();
    if (Lifecycle.BEFORE_INIT_EVENT.equals(type) && event.getLifecycle() instanceof Connector connector) {
      startFor(connector);
    } else if (Lifecycle.BEFORE_START_EVENT.equals(type)) {
      stop();
    } else if (Lifecycle.BEFORE_STOP_EVENT.equals(type) || Lifecycle.AFTER_DESTROY_EVENT.equals(type)) {
      stopAfterFailure();
    }
  }

  /**
   * @param enabledFlag false to never serve the loading page
   */
  public void setEnabled(boolean enabledFlag) {
    this.enabled = enabledFlag;
  }

  /**
   * @param directory directory containing optional branding files, defaults to
   *                  <code>${exo.conf.dir}/loading</code>
   */
  public void setConfDir(String directory) {
    this.confDir = directory;
  }

  /**
   * @param file platform log file in which the checkpoints are looked for,
   *             defaults to <code>${catalina.base}/logs/platform.log</code>
   */
  public void setLogFile(String file) {
    this.logFile = file;
  }

  /**
   * @param millis time the page keeps answering after the server decided to
   *               stop, to show the failure
   */
  void setFailureGraceMillis(long millis) {
    this.failureGraceMillis = millis;
  }

  private void startFor(Connector connector) {
    if (!isEnabled() || !(connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol)
        || protocol.isSSLEnabled()) {
      return;
    }
    int port = connector.getPortWithOffset();
    if (port <= 0) {
      return;
    }
    // Tomcat keeps the configured address of the connector as an InetAddress, null for all interfaces
    Object address = connector.getProperty("address");
    start(address instanceof InetAddress bindAddress ? bindAddress : null,
          port,
          connector.getService() == null ? null : connector.getService().getContainer());
  }

  synchronized void start(InetAddress address, int port, Engine engine) {
    stop();
    try {
      LoadingPageContent content = new LoadingPageContent(resolveConfDir());
      StartupProgress startupProgress = new StartupProgress();
      logTracker = new CheckpointTracker(content.getCheckpoints(), resolveLogFile());
      startupProgress.setCheckpoints(content.getCheckpoints(), logTracker);
      LoadingPageServer loadingServer = new LoadingPageServer(address, port, content, startupProgress);
      tracker = new DeploymentTracker(startupProgress);
      tracker.attach(engine);
      logTracker.start();
      loadingServer.start();
      progress = startupProgress;
      server = loadingServer;
      LOG.info("Serving the loading page on port " + port + " until the platform is started");
    } catch (Exception e) { // NOSONAR the platform must start even without the loading page
      LOG.warn("Loading page not started on port " + port + ": " + e);
      stop();
    }
  }

  /**
   * The server stops without having started the connector: the startup failed.
   * The page is told so and keeps answering a few seconds to show it.
   */
  synchronized void stopAfterFailure() {
    if (server != null && progress != null) {
      progress.fail();
      LOG.warn("The server is stopping before it was started, the loading page reports the failure");
      Timer closer = new Timer("loading-page-failure", true);
      closer.schedule(new TimerTask() {
        @Override
        public void run() {
          stop();
          closer.cancel();
        }
      }, failureGraceMillis);
    }
  }

  synchronized void stop() {
    if (logTracker != null) {
      logTracker.stop();
      logTracker = null;
    }
    progress = null;
    if (tracker != null) {
      tracker.detach();
      tracker = null;
    }
    if (server != null) {
      server.stop();
      server = null;
      LOG.info("Loading page stopped");
    }
  }

  private boolean isEnabled() {
    String flag = System.getProperty("exo.loading.page", System.getenv("EXO_LOADING_PAGE"));
    return enabled && !(flag != null && (flag.equalsIgnoreCase("false") || flag.equals("0") || flag.equalsIgnoreCase("no")));
  }

  private Path resolveLogFile() {
    if (logFile != null) {
      return Path.of(logFile);
    }
    String base = System.getProperty("catalina.base");
    return base == null ? null : Path.of(base, "logs", "platform.log");
  }

  private Path resolveConfDir() {
    if (confDir != null) {
      return Path.of(confDir);
    }
    String exoConfDir = System.getProperty("exo.conf.dir");
    return exoConfDir == null ? null : Path.of(exoConfDir, "loading");
  }

}
