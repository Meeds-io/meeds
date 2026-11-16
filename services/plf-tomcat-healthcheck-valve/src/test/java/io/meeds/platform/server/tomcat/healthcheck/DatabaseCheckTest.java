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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.DataSourceFactory;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.junit.jupiter.api.Test;

class DatabaseCheckTest {

  private static final int MAX_ACTIVE = 20;

  private static final int MAX_WAIT   = 10_000;

  @Test
  void aPoolSaturatedForLessThanMaxWaitIsBusyNotDownAndIsNotAsked() {
    Pool pool = new Pool(MAX_ACTIVE);
    assertNull(DatabaseCheck.check(pool, 0));
    assertNull(DatabaseCheck.check(pool, 9_999_000_000L));
    assertEquals(0, pool.borrows.get(), "no waiter joins the queue of a saturated pool");
  }

  @Test
  void aPoolThatStaysSaturatedForMaxWaitIsReportedAndStillNotAsked() {
    Pool pool = new Pool(MAX_ACTIVE);
    assertNull(DatabaseCheck.check(pool, 0));
    assertEquals("has had every connection in use for over 10 s", DatabaseCheck.check(pool, 10_000_000_000L));
    assertEquals("has had every connection in use for over 10 s", DatabaseCheck.check(pool, 60_000_000_000L));
    assertEquals(0, pool.borrows.get());
  }

  @Test
  void aPoolSeenFullTwiceWhileItsConnectionsWentBackAndForthIsNotReported() {
    Pool pool = new Pool(MAX_ACTIVE);
    assertNull(DatabaseCheck.check(pool, 0));
    // Every connection was returned and borrowed again between the two evaluations: it is busy, not stuck
    pool.returned = 40;
    assertNull(DatabaseCheck.check(pool, 10_000_000_000L));
    assertNull(DatabaseCheck.check(pool, 19_000_000_000L));
    // No connection came back since the second sighting for 10 s: now it stays saturated
    assertEquals("has had every connection in use for over 10 s", DatabaseCheck.check(pool, 20_000_000_000L));
    // The pool resets its statistics: the count moved, so it starts again
    pool.returned = 0;
    assertNull(DatabaseCheck.check(pool, 21_000_000_000L));
  }

  @Test
  void aPoolThatFreesAConnectionStartsItsCountAgain() {
    Pool pool = new Pool(MAX_ACTIVE);
    assertNull(DatabaseCheck.check(pool, 0));
    pool.active = MAX_ACTIVE - 1;
    assertEquals("is unreachable", DatabaseCheck.check(pool, 8_000_000_000L));
    pool.active = MAX_ACTIVE;
    assertNull(DatabaseCheck.check(pool, 9_000_000_000L));
    // 11 s after the first evaluation, but only 2 s since the pool was saturated again
    assertNull(DatabaseCheck.check(pool, 11_000_000_000L));
    assertEquals("has had every connection in use for over 10 s", DatabaseCheck.check(pool, 19_000_000_000L));
  }

  @Test
  void aPoolWithFreeConnectionsIsAskedAndADownDatabaseIsReported() {
    Pool pool = new Pool(MAX_ACTIVE - 1);
    assertEquals("is unreachable", DatabaseCheck.check(pool));
    assertEquals(1, pool.borrows.get());
  }

  @Test
  void theServerTemplateDeclaresTheAbandonAttributesTomcatJdbcReads() throws IOException {
    Path template = Path.of("..", "..", "packaging", "plf-tomcat-resources", "src", "main", "resources", "conf", "server.template.xml");
    assumeTrue(Files.exists(template), "the packaging module is not checked out");
    String xml = Files.readString(template);
    // These are DBCP2 names: the factory of tomcat-jdbc ignores them and keeps removeAbandoned=false
    assertEquals(0, count(xml, "removeAbandonedOnBorrow"));
    assertEquals(0, count(xml, "removeAbandonedOnMaintenance"));
    assertEquals(3, count(xml, "removeAbandoned=\"true\" removeAbandonedTimeout=\"300\""), "the three pools");
    // Without it the pool closes any connection held for 300 s, the one of a long changelog included
    assertEquals(3, count(xml, "abandonWhenPercentageFull=\"100\""), "abandon only while the pool is full");
    Properties declared = new Properties();
    declared.setProperty("removeAbandoned", "true");
    declared.setProperty("removeAbandonedTimeout", "300");
    declared.setProperty("abandonWhenPercentageFull", "100");
    PoolConfiguration pool = DataSourceFactory.parsePoolProperties(declared);
    assertTrue(pool.isRemoveAbandoned());
    assertEquals(300, pool.getRemoveAbandonedTimeout());
    assertEquals(100, pool.getAbandonWhenPercentageFull());
    Properties dbcp2 = new Properties();
    dbcp2.setProperty("removeAbandonedOnBorrow", "true");
    assertFalse(DataSourceFactory.parsePoolProperties(dbcp2).isRemoveAbandoned());
  }

  private static int count(String text, String part) {
    return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
  }

  /** A tomcat-jdbc pool whose state is set by the test and whose database is down. */
  private static class Pool extends DataSource {

    private int             active;

    private long            returned;

    private final AtomicInteger borrows = new AtomicInteger();

    Pool(int active) {
      this.active = active;
    }

    @Override
    public int getActive() {
      return active;
    }

    @Override
    public long getReturnedCount() {
      return returned;
    }

    @Override
    public int getMaxActive() {
      return MAX_ACTIVE;
    }

    @Override
    public int getMaxWait() {
      return MAX_WAIT;
    }

    @Override
    public java.sql.Connection getConnection() throws SQLException {
      borrows.incrementAndGet();
      throw new SQLException("down");
    }
  }
}
