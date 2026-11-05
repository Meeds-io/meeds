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

/**
 * A static file served by the loading page.
 *
 * @see LoadingPageContent#asset(String)
 */
public final class Asset {

  /** Content of the file. */
  private final byte[] bytes;

  /** Media type of the file. */
  private final String contentType;

  Asset(byte[] content, String type) {
    this.bytes = content.clone();
    this.contentType = type;
  }

  public byte[] bytes() {
    return bytes.clone();
  }

  public String contentType() {
    return contentType;
  }
}
