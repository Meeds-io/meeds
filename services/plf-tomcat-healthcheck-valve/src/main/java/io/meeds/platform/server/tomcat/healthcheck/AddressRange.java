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

/** A network, an address being a network of one address. */
final class AddressRange {

  /** Bits of a byte. */
  private static final int BITS = 8;

  /** Mask of the bits of a byte. */
  private static final int BYTE_MASK = 0xFF;

  /** Bytes of the network address. */
  private final byte[] network;

  /** Number of leading bits that are compared. */
  private final int    prefix;

  AddressRange(byte[] address, int bits) {
    this.network = address;
    this.prefix = bits;
  }

  boolean matches(byte[] address) {
    if (address.length != network.length) {
      return false;
    }
    int full = prefix / BITS;
    for (int i = 0; i < full; i++) {
      if (address[i] != network[i]) {
        return false;
      }
    }
    int rest = prefix % BITS;
    if (rest == 0) {
      return true;
    }
    int mask = BYTE_MASK << (BITS - rest) & BYTE_MASK;
    return (address[full] & mask) == (network[full] & mask);
  }
}
