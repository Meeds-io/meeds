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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * List of addresses, or CIDR networks, allowed to call the health check.
 * Entries are IP literals only, a host name is never resolved.
 */
final class AddressAllowList {

  /** Dotted IPv4 address, every number from 0 to 255. */
  private static final Pattern IPV4 = Pattern.compile("(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}");

  /** Result for a text that is not an IP literal. */
  private static final byte[] NO_ADDRESS = new byte[0];

  /** Bits in a byte. */
  private static final int BITS = 8;

  /** Radix of the hexadecimal digits. */
  private static final int HEX = 16;

  /** Allowed addresses and networks. */
  private final List<AddressRange> entries = new ArrayList<>();

  /**
   * @param spec comma separated IP addresses or CIDR networks, for example
   *             <code>127.0.0.1,::1,172.17.0.0/16</code>
   * @throws IllegalArgumentException when an entry is not an IP literal
   */
  AddressAllowList(String spec) {
    for (String item : spec.split(",")) {
      String value = item.trim();
      if (!value.isEmpty()) {
        entries.add(parse(value));
      }
    }
  }

  /**
   * @param address IP literal of the caller
   * @return true when the address is in the list
   */
  boolean allows(String address) {
    byte[] bytes = toBytes(address);
    if (bytes.length == 0) {
      return false;
    }
    for (AddressRange entry : entries) {
      if (entry.matches(bytes)) {
        return true;
      }
    }
    return false;
  }

  private static AddressRange parse(String value) {
    int slash = value.indexOf('/');
    String host = slash < 0 ? value : value.substring(0, slash);
    byte[] bytes = toBytes(host);
    if (bytes.length == 0) {
      throw new IllegalArgumentException("Not an IP address: " + value);
    }
    int prefix = bytes.length * BITS;
    if (slash >= 0) {
      try {
        prefix = Integer.parseInt(value.substring(slash + 1));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid network prefix: " + value, e);
      }
      if (prefix < 0 || prefix > bytes.length * BITS) {
        throw new IllegalArgumentException("Invalid network prefix: " + value);
      }
    }
    return new AddressRange(bytes, prefix);
  }

  /**
   * Converts an IP literal to bytes, an IPv4 mapped IPv6 address becoming IPv4.
   *
   * @param address IP literal
   * @return the bytes, empty when the address is not an IP literal
   */
  private static byte[] toBytes(String address) {
    if (address == null || address.isEmpty() || !isLiteral(address)) {
      return NO_ADDRESS;
    }
    try {
      return InetAddress.getByName(address).getAddress();
    } catch (UnknownHostException e) {
      return NO_ADDRESS;
    }
  }

  /**
   * Avoids any DNS lookup: {@link InetAddress#getByName(String)} resolves the
   * text it can not read as an address, so only a strict dotted IPv4 address or
   * a text with a colon (read as IPv6, never resolved) goes through.
   *
   * @param address text to check
   * @return true when the text may be an IP literal
   */
  private static boolean isLiteral(String address) {
    if (IPV4.matcher(address).matches()) {
      return true;
    }
    if (address.indexOf(':') < 0) {
      return false;
    }
    for (int i = 0; i < address.length(); i++) {
      char c = address.charAt(i);
      if (!(Character.digit(c, HEX) >= 0 || c == '.' || c == ':')) {
        return false;
      }
    }
    return true;
  }
}
