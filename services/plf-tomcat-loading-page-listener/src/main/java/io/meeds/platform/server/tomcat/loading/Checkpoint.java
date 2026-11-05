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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A step of the startup shown on the loading page: an id and what tells that
 * it is reached.
 *
 * @param id      identifier, also used to find the label in the language
 *                bundles
 * @param pattern text to find in a line of the platform log, null for a
 *                built-in checkpoint
 * @param builtin {@link #START} or {@link #DEPLOYED} for a built-in
 *                checkpoint, null otherwise
 */
public record Checkpoint(String id, Pattern pattern, String builtin) {

  /** Built-in value: reached when the loading page starts. */
  public static final String START        = "@start";

  /** Built-in value: reached when all the web applications are deployed. */
  public static final String DEPLOYED     = "@deployed";

  /** Name of the file listing the checkpoints. */
  public static final String CONFIG_FILE  = "checkpoints.conf";

  /** Accepted ids: they end up in the HTML and in the JSON. */
  private static final Pattern VALID_ID   = Pattern.compile("[A-Za-z0-9_-]{1,40}");

  /** Maximum size of the checkpoints file. */
  private static final long    MAX_BYTES  = 64L * 1024;

  /**
   * Loads the checkpoints from the configuration directory, or from the
   * default ones of the jar when there is no such file.
   *
   * @param  confDir directory that may contain {@link #CONFIG_FILE}, may be
   *                 null
   * @return         the checkpoints in their display order
   */
  public static List<Checkpoint> load(Path confDir) {
    return load(confDir, Checkpoint.class.getClassLoader());
  }

  /**
   * @param  confDir directory that may contain {@link #CONFIG_FILE}, may be
   *                 null
   * @param  loader  class loader of the default checkpoints
   * @return         the checkpoints in their display order
   */
  static List<Checkpoint> load(Path confDir, ClassLoader loader) {
    String text = null;
    try {
      Path file = confDir == null ? null : confDir.resolve(CONFIG_FILE);
      if (file != null && Files.isRegularFile(file) && Files.size(file) <= MAX_BYTES) {
        text = Files.readString(file, StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      // Unreadable file: the default checkpoints are used
    }
    if (text == null) {
      try (var in = loader.getResourceAsStream("loading/" + CONFIG_FILE)) {
        text = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        text = "";
      }
    }
    return parse(text);
  }

  /**
   * @param  text content of a checkpoints file: one <code>id=pattern</code> per
   *              line, blank lines and lines starting with # are ignored
   * @return      the valid checkpoints, invalid lines are skipped
   */
  public static List<Checkpoint> parse(String text) {
    List<Checkpoint> checkpoints = new ArrayList<>();
    for (String line : text.split("\\R")) {
      int index = line.indexOf('=');
      String trimmed = line.trim();
      if (index > 0 && !trimmed.startsWith("#")) {
        String id = line.substring(0, index).trim();
        String value = line.substring(index + 1).trim();
        if (VALID_ID.matcher(id).matches() && !value.isEmpty() && checkpoints.stream().noneMatch(c -> c.id().equals(id))) {
          addCheckpoint(checkpoints, id, value);
        }
      }
    }
    return checkpoints;
  }

  private static void addCheckpoint(List<Checkpoint> checkpoints, String id, String value) {
    if (START.equals(value) || DEPLOYED.equals(value)) {
      checkpoints.add(new Checkpoint(id, null, value));
    } else {
      try {
        checkpoints.add(new Checkpoint(id, Pattern.compile(value), null));
      } catch (PatternSyntaxException e) {
        // Invalid expression: the checkpoint is skipped
      }
    }
  }

}
