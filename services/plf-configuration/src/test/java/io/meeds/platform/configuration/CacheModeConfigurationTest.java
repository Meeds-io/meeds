/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2026 Meeds Association
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
package io.meeds.platform.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pins the cache modes the platform ships in
 * {@code conf/platform/configuration.properties}: the groovy templates stay
 * local, and no cache replicates but the one that holds state no store keeps.
 */
class CacheModeConfigurationTest {

  private static final String      CONFIGURATION          = "conf/platform/configuration.properties";

  private static final String      TEMPLATE_KEY           = "exo.cache.portal.template.cacheMode";

  private static final String      REPLICATED_KEY         = "exo.cache.commons.UserStateService.cacheMode";

  /** The modes of the kernel's CacheMode that copy values to the other nodes. */
  private static final Set<String> REPLICATION_MODES      = Set.of("replication", "asyncreplication", "distributed");

  private static Properties        properties;

  @BeforeAll
  static void load() throws IOException {
    properties = new Properties();
    try (InputStream in = CacheModeConfigurationTest.class.getClassLoader().getResourceAsStream(CONFIGURATION)) {
      assertNotNull(in, CONFIGURATION + " is not on the classpath");
      properties.load(in);
    }
  }

  @Test
  void templateCacheStaysLocal() {
    assertEquals("local", properties.getProperty(TEMPLATE_KEY));
  }

  @Test
  void onlyUserStateCacheReplicates() {
    List<String> replicated = properties.stringPropertyNames()
                                        .stream()
                                        .filter(key -> key.endsWith(".cacheMode"))
                                        .filter(key -> !REPLICATED_KEY.equals(key))
                                        .filter(key -> REPLICATION_MODES.contains(properties.getProperty(key)
                                                                                            .trim()
                                                                                            .toLowerCase(Locale.ROOT)))
                                        .sorted()
                                        .toList();
    assertTrue(replicated.isEmpty(), "Caches shipped with a replication mode: " + replicated);
    assertTrue(properties.containsKey(REPLICATED_KEY), REPLICATED_KEY + " is expected to be set");
  }

}
