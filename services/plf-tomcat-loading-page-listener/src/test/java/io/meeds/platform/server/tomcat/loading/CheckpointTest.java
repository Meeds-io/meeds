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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckpointTest {

  @TempDir
  Path dir;

  @Test
  void parsesTheFileAndSkipsWhatIsInvalid() {
    List<Checkpoint> list = Checkpoint.parse("""
        # a comment
        server=@start

        applications=@deployed
        kernel=Building root container
        broken id=text
        bad-regex=([unclosed
        =no id
        empty=
        kernel=duplicated id is ignored
        db = EntityManagerFactory \\[meeds-jpa\\]
        """);
    assertEquals(List.of("server", "applications", "kernel", "db"), list.stream().map(Checkpoint::id).toList());
    assertEquals(Checkpoint.START, list.get(0).builtin());
    assertEquals(Checkpoint.DEPLOYED, list.get(1).builtin());
    assertNull(list.get(2).builtin());
    assertTrue(list.get(3).pattern().matcher("EntityManagerFactory [meeds-jpa] - Creating").find());
  }

  @Test
  void defaultCheckpointsAreInTheJar() {
    List<Checkpoint> list = Checkpoint.load(dir);
    assertEquals("server", list.get(0).id());
    assertTrue(list.size() >= 6);
  }

  @Test
  void aDistributionCanReplaceTheCheckpoints() throws IOException {
    Files.writeString(dir.resolve(Checkpoint.CONFIG_FILE), "only=Only this one\n");
    assertEquals(List.of("only"), Checkpoint.load(dir).stream().map(Checkpoint::id).toList());
  }

  @Test
  void everyDefaultCheckpointHasALabel() {
    LoadingPageContent content = new LoadingPageContent(dir);
    for (Locale locale : LoadingPageContent.AVAILABLE) {
      String page = content.page(locale);
      for (Checkpoint checkpoint : content.getCheckpoints()) {
        String item = "<li data-id=\"" + checkpoint.id() + "\">";
        assertTrue(page.contains(item), "Missing item " + checkpoint.id() + " in " + locale);
        // the label is not the raw id
        int start = page.indexOf(item) + item.length();
        assertNotEquals(checkpoint.id(), page.substring(start, page.indexOf("</li>", start)),
                    "No label for " + checkpoint.id() + " in " + locale);
      }
    }
  }

  @Test
  void trackerFindsCheckpointsInAGrowingLog() throws IOException {
    Path log = dir.resolve("platform.log");
    Files.writeString(log, "old run: Building root container\n");
    CheckpointTracker tracker = new CheckpointTracker(Checkpoint.parse("kernel=Building root container\nsearch=Use ES Version\n"), log);
    tracker.poll();
    assertFalse(tracker.isReached("kernel"), "Lines written before the start must be ignored");

    append(log, "2026-10-20 | INFO  | Building root container [x]\n");
    tracker.poll();
    assertTrue(tracker.isReached("kernel"));
    assertFalse(tracker.isReached("search"));

    // a line written in two parts, with colors and accents
    append(log, "2026 | INFO  | \u001B[32mUse ES Ver");
    tracker.poll();
    assertFalse(tracker.isReached("search"));
    append(log, "sion\u001B[0m 9.4 é\n");
    tracker.poll();
    assertTrue(tracker.isReached("search"));
  }

  @Test
  void trackerFollowsARotatedLogAndAFileCreatedLater() throws IOException {
    Path log = dir.resolve("platform.log");
    CheckpointTracker tracker = new CheckpointTracker(Checkpoint.parse("a=first\nb=second\n"), log);
    tracker.poll(); // the file does not exist yet
    append(log, "the first line is long enough to be bigger than what follows\n");
    tracker.poll();
    assertTrue(tracker.isReached("a"));

    Files.writeString(log, "second\n"); // rotated: the new file is shorter
    tracker.poll();
    assertTrue(tracker.isReached("b"));
  }

  @Test
  void statusListsTheReachedCheckpointsInOrder() throws IOException {
    Path log = dir.resolve("platform.log");
    List<Checkpoint> list = Checkpoint.parse("server=@start\napplications=@deployed\nkernel=kernel up\nsearch=search up\n");
    CheckpointTracker tracker = new CheckpointTracker(list, log);
    StartupProgress progress = new StartupProgress();
    progress.setCheckpoints(list, tracker);
    assertTrue(progress.toJson(0).endsWith("\"checkpoints\":[\"server\"]}"), progress.toJson(0));

    append(log, "search up\n");
    tracker.poll();
    progress.setTotal(1);
    progress.applicationSeen();
    progress.applicationStarted();
    assertTrue(progress.toJson(0).endsWith("\"checkpoints\":[\"server\",\"applications\",\"search\"]}"), progress.toJson(0));
  }

  @Test
  void failedPhaseWinsOverTheDeployment() {
    StartupProgress progress = new StartupProgress();
    progress.setTotal(1);
    progress.applicationSeen();
    assertEquals(StartupProgress.PHASE_DEPLOYING, progress.getPhase());
    progress.fail();
    assertEquals(StartupProgress.PHASE_FAILED, progress.getPhase());
    assertTrue(progress.toJson(5).contains("\"phase\":\"failed\""));
  }

  @Test
  void aCheckpointsFileThatCanNotBeReadIsIgnored() throws IOException {
    Path file = dir.resolve(Checkpoint.CONFIG_FILE);
    Files.writeString(file, "only=Only this one\n");
    Files.setPosixFilePermissions(file, java.util.Set.of());
    Assumptions.assumeFalse(Files.isReadable(file), "Permissions are not enforced for this user");
    assertEquals("server", Checkpoint.load(dir).get(0).id());
  }

  @Test
  void defaultCheckpointsThatAreMissingOrBrokenGiveNoCheckpoint() {
    assertTrue(Checkpoint.load(dir, new ClassLoader(null) { }).isEmpty());
    ClassLoader broken = new ClassLoader(null) {
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
    assertTrue(Checkpoint.load(dir, broken).isEmpty());
  }

  @Test
  void trackerReadsTheLogInTheBackground() throws Exception {
    Path log = dir.resolve("platform.log");
    Files.writeString(log, "");
    CheckpointTracker tracker = new CheckpointTracker(Checkpoint.parse("kernel=Building root container\nsearch=Use ES Version\n"), log);
    tracker.start();
    tracker.start(); // already running
    try {
      append(log, "2026 | INFO  | Building root container [x]\n");
      assertTrue(awaitReached(tracker, "kernel"));
      // a second line is read by a later poll of the same thread
      append(log, "2026 | INFO  | Use ES Version 9.4 [x]\n");
      assertTrue(awaitReached(tracker, "search"));
    } finally {
      tracker.stop();
      tracker.stop(); // already stopped
    }
  }

  private static boolean awaitReached(CheckpointTracker tracker, String id) {
    long limit = System.nanoTime() + 5_000_000_000L;
    while (!tracker.isReached(id) && System.nanoTime() < limit) {
      java.util.concurrent.locks.LockSupport.parkNanos(25_000_000L);
    }
    return tracker.isReached(id);
  }

  @Test
  void trackerIgnoresALogThatCanNotBeReadOrIsMissing() throws IOException {
    new CheckpointTracker(Checkpoint.parse("a=text\n"), null).poll();

    Path log = dir.resolve("platform.log");
    Files.writeString(log, "");
    CheckpointTracker tracker = new CheckpointTracker(Checkpoint.parse("a=text\n"), log);
    append(log, "text\n");
    Files.setPosixFilePermissions(log, java.util.Set.of());
    Assumptions.assumeFalse(Files.isReadable(log), "Permissions are not enforced for this user");
    tracker.poll();
    assertFalse(tracker.isReached("a"));
  }

  private static void append(Path file, String text) throws IOException {
    Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

}
