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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeploymentTrackerTest {

  @TempDir
  Path                appBase;

  private StartupProgress progress = new StartupProgress();

  private DeploymentTracker tracker = new DeploymentTracker(progress);

  private StandardHost    host;

  @BeforeEach
  void setUp() throws IOException {
    Path webapps = appBase.resolve("webapps");
    Files.createDirectories(webapps.resolve("portal/WEB-INF"));
    Files.createDirectories(webapps.resolve("social"));
    Files.createDirectories(webapps.resolve("META-INF"));
    Files.createFile(webapps.resolve("news.war"));
    Files.createDirectories(webapps.resolve("news")); // exploded war, counted once
    host = new StandardHost();
    host.setName("localhost");
    host.setAppBase(webapps.toString());
    StandardServer server = new StandardServer();
    server.setCatalinaBase(appBase.resolve("base").toFile());
    StandardService service = new StandardService();
    service.setServer(server);
    StandardEngine engine = new StandardEngine();
    engine.setName("Catalina");
    engine.setService(service);
    engine.addChild(host);
  }

  @Test
  void countsWhatTomcatIsGoingToDeploy() throws IOException {
    assertEquals(3, DeploymentTracker.countExpected(host));
    // context descriptors: one more application, and one that is also a folder
    Path descriptors = appBase.resolve("base/conf/Catalina/localhost");
    Files.createDirectories(descriptors);
    Files.createFile(descriptors.resolve("extra.xml"));
    Files.createFile(descriptors.resolve("portal.xml"));
    Files.createFile(descriptors.resolve("readme.txt"));
    assertEquals(4, DeploymentTracker.countExpected(host));
    tracker.attach((StandardEngine) host.getParent());
    assertTrue(progress.toJson(0).contains("\"total\":4"), progress.toJson(0));
  }

  @Test
  void nothingToFollowWithoutEngine() {
    tracker.attach(null);
    assertEquals(StartupProgress.PHASE_STARTING, progress.getPhase());
  }

  @Test
  void ignoresWhatIsNotTheStartOfAContext() {
    tracker.attach((StandardEngine) host.getParent());
    StandardContext context = context("/portal");
    fire(context, Lifecycle.START_EVENT);
    for (var listener : context.findLifecycleListeners()) {
      if (listener.getClass().getName().startsWith(DeploymentTracker.class.getName())) {
        // the event of something that is not a context
        listener.lifecycleEvent(new LifecycleEvent(host, Lifecycle.AFTER_START_EVENT, null));
      }
    }
    host.removeChild(context);
    assertTrue(progress.toJson(0).contains("\"deployed\":0"), progress.toJson(0));
  }

  @Test
  void detachRemovesTheListenersOfTheContexts() {
    tracker.attach((StandardEngine) host.getParent());
    StandardContext context = context("/portal");
    int before = context.findLifecycleListeners().length;
    tracker.detach();
    assertEquals(before - 1, context.findLifecycleListeners().length);
  }

  @Test
  void followsTheDeploymentUntilTheServicesStart() {
    tracker.attach((StandardEngine) host.getParent());
    assertEquals(StartupProgress.PHASE_STARTING, progress.getPhase());

    StandardContext portal = context("/portal");
    StandardContext social = context("/social");
    StandardContext news = context("/news");
    assertEquals(StartupProgress.PHASE_DEPLOYING, progress.getPhase());

    started(portal);
    started(social);
    assertEquals(StartupProgress.PHASE_DEPLOYING, progress.getPhase());
    assertTrue(progress.toJson(0).contains("\"deployed\":2"));

    started(news);
    assertEquals(StartupProgress.PHASE_SERVICES, progress.getPhase());
  }

  @Test
  void aContextStoppedBeforeBeingStartedIsAFailure() {
    tracker.attach((StandardEngine) host.getParent());
    StandardContext broken = context("/portal");
    fire(broken, Lifecycle.BEFORE_STOP_EVENT);
    // Stopping a second time or after a start is not counted again
    fire(broken, Lifecycle.BEFORE_STOP_EVENT);
    StandardContext social = context("/social");
    started(social);
    fire(social, Lifecycle.BEFORE_STOP_EVENT);
    String json = progress.toJson(0);
    assertTrue(json.contains("\"failed\":1"), json);
    assertTrue(json.contains("\"deployed\":1"), json);
  }

  @Test
  void detachStopsFollowingTheHost() {
    tracker.attach((StandardEngine) host.getParent());
    tracker.detach();
    context("/portal");
    assertEquals(StartupProgress.PHASE_STARTING, progress.getPhase());
  }

  @Test
  void aMissingAppBaseGivesAnUnknownTotal() {
    StandardHost other = new StandardHost();
    other.setName("other");
    other.setAppBase(new File(appBase.toFile(), "missing").getPath());
    assertEquals(0, DeploymentTracker.countExpected(other));
  }

  private StandardContext context(String path) {
    StandardContext context = new StandardContext();
    context.setPath(path);
    host.addChild(context);
    return context;
  }

  private void started(StandardContext context) {
    fire(context, Lifecycle.AFTER_START_EVENT);
  }

  private void fire(StandardContext context, String type) {
    for (var listener : context.findLifecycleListeners()) {
      // Only the listener of the tracker, not the ones of Tomcat that need a started context
      if (listener.getClass().getName().startsWith(DeploymentTracker.class.getName())) {
        listener.lifecycleEvent(new LifecycleEvent(context, type, null));
      }
    }
  }

}
