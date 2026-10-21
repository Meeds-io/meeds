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
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

/**
 * Follows the end of the platform log while the platform is starting and
 * remembers which checkpoints appeared in it. It only reads the file, from the
 * position it had when the tracker was created.
 */
public final class CheckpointTracker {

  /** Delay between two reads of the log file, in nanoseconds. */
  private static final long    POLL_NANOS     = 500_000_000L;

  /** Maximum number of bytes read at once. */
  private static final int     MAX_CHUNK      = 1024 * 1024;

  /** Color codes that the console appenders may write in the log. */
  private static final Pattern ANSI           = Pattern.compile("\u001B\\[[0-9;]*m");

  /** Checkpoints to find in the log. */
  private final List<Checkpoint> checkpoints;

  /** Log file followed, may not exist yet. */
  private final Path           logFile;

  /** Ids of the checkpoints found in the log. */
  private final Set<String>    reached        = ConcurrentHashMap.newKeySet();

  /** Position of the next byte to read. */
  private long                 position;

  /** Bytes of a line which is not complete yet. */
  private byte[]               pending        = new byte[0];

  /** Thread reading the file, empty when not started. */
  private final AtomicReference<Thread> thread = new AtomicReference<>();

  /**
   * @param checkpointList checkpoints to find
   * @param file           log file to follow
   */
  public CheckpointTracker(List<Checkpoint> checkpointList, Path file) {
    this.checkpoints = checkpointList;
    this.logFile = file;
    this.position = size();
  }

  /**
   * @param  id checkpoint id
   * @return    true when its text was found in the log
   */
  public boolean isReached(String id) {
    return reached.contains(id);
  }

  /**
   * Starts reading the log file in the background.
   */
  public void start() {
    Thread reader = new Thread(this::run, "loading-page-log");
    reader.setDaemon(true);
    if (thread.compareAndSet(null, reader)) {
      reader.start();
    }
  }

  /**
   * Stops reading the log file.
   */
  public void stop() {
    Thread reader = thread.getAndSet(null);
    if (reader != null) {
      LockSupport.unpark(reader);
    }
  }

  /**
   * Reads what was appended to the log since the last read.
   */
  synchronized void poll() {
    long size = size();
    if (size < position) {
      // The file was rotated or truncated: read the new one from its start
      position = 0;
      pending = new byte[0];
    }
    if (size > position) {
      try (RandomAccessFile file = new RandomAccessFile(logFile.toFile(), "r")) {
        byte[] chunk = new byte[(int) Math.min(size - position, MAX_CHUNK)];
        file.seek(position);
        file.readFully(chunk);
        position += chunk.length;
        consume(chunk);
      } catch (IOException e) {
        // Try again at the next poll
      }
    }
  }

  /**
   * Looks for the checkpoints in a line of log.
   *
   * @param line a line of the log
   */
  void scan(String line) {
    String text = ANSI.matcher(line).replaceAll("");
    for (Checkpoint checkpoint : checkpoints) {
      if (checkpoint.pattern() != null && !reached.contains(checkpoint.id()) && checkpoint.pattern().matcher(text).find()) {
        reached.add(checkpoint.id());
      }
    }
  }

  private void consume(byte[] chunk) {
    byte[] data = new byte[pending.length + chunk.length];
    System.arraycopy(pending, 0, data, 0, pending.length);
    System.arraycopy(chunk, 0, data, pending.length, chunk.length);
    int start = 0;
    for (int i = 0; i < data.length; i++) {
      if (data[i] == '\n') {
        scan(new String(data, start, i - start, StandardCharsets.UTF_8));
        start = i + 1;
      }
    }
    pending = java.util.Arrays.copyOfRange(data, start, data.length);
  }

  private void run() {
    while (thread.get() == Thread.currentThread()) {
      poll();
      LockSupport.parkNanos(POLL_NANOS);
    }
  }

  private long size() {
    return logFile != null && logFile.toFile().isFile() ? logFile.toFile().length() : 0;
  }

}
