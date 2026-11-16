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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AddressAllowListTest {

  @Test
  void matchesExactAddressesAndNetworks() {
    AddressAllowList list = new AddressAllowList("127.0.0.1, ::1,172.17.0.0/16,fd00::/8");
    assertTrue(list.allows("127.0.0.1"));
    assertTrue(list.allows("0:0:0:0:0:0:0:1"));
    assertTrue(list.allows("172.17.5.9"));
    assertTrue(list.allows("fd12::5"));
    assertFalse(list.allows("127.0.0.2"));
    assertFalse(list.allows("172.18.0.1"));
    assertFalse(list.allows("fe80::1"));
  }

  @Test
  void treatsAMappedIpv4AddressAsIpv4() {
    assertTrue(new AddressAllowList("127.0.0.1").allows("::ffff:127.0.0.1"));
    assertFalse(new AddressAllowList("127.0.0.1").allows("::ffff:10.0.0.1"));
  }

  @Test
  void neverAllowsAnythingUnparsableNorResolvesNames() {
    AddressAllowList list = new AddressAllowList("127.0.0.1");
    assertFalse(list.allows(null));
    assertFalse(list.allows(""));
    assertFalse(list.allows("localhost"));
    assertFalse(new AddressAllowList("").allows("127.0.0.1"));
  }

  @Test
  void comparesOnlyTheBitsOfThePrefix() {
    AddressAllowList list = new AddressAllowList("172.16.0.0/12");
    assertTrue(list.allows("172.31.255.255"));
    assertFalse(list.allows("172.32.0.1"));
    assertFalse(list.allows("172.15.255.255"));
  }

  @Test
  void neverResolvesATextThatLooksLikeAHostName() {
    // Only hex digits, dots or an invalid number: InetAddress would look them up in the DNS
    for (String text : new String[] { "cafe", "deadbeef.add", "999.1.1.1", "1.2.3", "10.0.0.1.5", "a.b.c.d" }) {
      assertFalse(new AddressAllowList("127.0.0.1").allows(text), text);
      assertThrows(IllegalArgumentException.class, () -> new AddressAllowList(text), text);
    }
  }

  @Test
  void anInvalidIpv6LiteralIsNotAnAddress() {
    assertFalse(new AddressAllowList("::1").allows(":::"));
    // A zone id, or any other text, after the colons is not an address either
    assertFalse(new AddressAllowList("::1").allows("::1%eth0"));
    assertFalse(new AddressAllowList("::1").allows("::g"));
    assertThrows(IllegalArgumentException.class, () -> new AddressAllowList("1:2:3:4:5:6:7:8:9"));
  }

  @Test
  void rejectsInvalidConfiguration() {
    assertThrows(IllegalArgumentException.class, () -> new AddressAllowList("localhost"));
    assertThrows(IllegalArgumentException.class, () -> new AddressAllowList("10.0.0.0/33"));
    assertThrows(IllegalArgumentException.class, () -> new AddressAllowList("10.0.0.0/x"));
  }
}
