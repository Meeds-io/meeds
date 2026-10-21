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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Builds the content of the page served while the platform is starting: the
 * localized HTML page (texts come from the Java resource bundle
 * <code>locale/loading/loading_*.properties</code>) and its static assets.
 * The logo, the colors and any style can be replaced by files of the
 * configuration directory, see {@link #asset(String)}.
 */
public final class LoadingPageContent {

  /** Base name of the resource bundles, translated using Crowdin. */
  static final String                 BUNDLE_NAME     = "locale.loading.loading";

  /** Languages having a bundle, found in the classpath. The first one is the default and fallback. */
  static final List<Locale>           AVAILABLE       = findAvailableLocales(LoadingPageContent.class.getClassLoader());

  /** Languages written from right to left. */
  private static final Set<String>    RTL_LANGUAGES   = Set.of("ar", "he", "fa", "ur");

  /** Accepted colors of the branding. */
  private static final Pattern        COLOR           = Pattern.compile("#[0-9a-fA-F]{3,8}");

  /** No content: there is no such file. */
  private static final byte[] NO_BYTES    = new byte[0];

  /** Room kept for the entities when escaping a text. */
  private static final int ESCAPE_EXTRA = 8;

  /** Maximum size of a file replacing a default one. */
  private static final long           MAX_ASSET_BYTES = 2L * 1024 * 1024;

  /** Media type of each served file. */
  private static final Map<String, String> CONTENT_TYPES = Map.of("loading.css", "text/css; charset=UTF-8",
                                                                  "theme.css", "text/css; charset=UTF-8",
                                                                  "custom.css", "text/css; charset=UTF-8",
                                                                  "loading.js", "text/javascript; charset=UTF-8",
                                                                  "logo.png", "image/png",
                                                                  "logo-dark.png", "image/png");

  /** Directory of the optional branding files. */
  private final Path                  confDir;

  /** Branding properties found in the configuration directory. */
  private final Properties            branding;

  /** HTML template of the page. */
  private final String                template;

  /** Checkpoints of the startup listed on the page. */
  private final List<Checkpoint>      checkpoints;

  /** Rendered pages by language. */
  private final Map<Locale, String>   pages           = new ConcurrentHashMap<>();

  /**
   * @param directory directory where the optional branding files are looked up
   *                  (logo.png, logo-dark.png, branding.properties), may be
   *                  null or missing
   */
  public LoadingPageContent(Path directory) {
    this.confDir = directory;
    this.branding = loadBranding(directory);
    this.template = new String(classpathBytes("loading/index.html"), StandardCharsets.UTF_8);
    this.checkpoints = Checkpoint.load(directory);
  }

  /**
   * @return the checkpoints of the startup, in their display order
   */
  public List<Checkpoint> getCheckpoints() {
    return checkpoints;
  }

  /**
   * Picks the supported language. The <code>lang</code> query parameter wins
   * over the Accept-Language header, English is used when nothing matches.
   *
   * @param acceptLanguage value of the Accept-Language header, may be null
   * @param langParameter  value of the lang query parameter, may be null
   * @return one of {@link #AVAILABLE}
   */
  public static Locale resolveLocale(String acceptLanguage, String langParameter) {
    if (langParameter != null && !langParameter.isBlank()) {
      Locale supported = supported(langParameter.trim());
      if (supported != null) {
        return supported;
      }
    }
    if (acceptLanguage != null && !acceptLanguage.isBlank()) {
      try {
        List<Locale.LanguageRange> ranges = new ArrayList<>(Locale.LanguageRange.parse(acceptLanguage));
        ranges.sort(Comparator.comparingDouble(Locale.LanguageRange::getWeight).reversed());
        for (Locale.LanguageRange range : ranges) {
          Locale supported = supported(range.getRange());
          if (supported != null) {
            return supported;
          }
        }
      } catch (IllegalArgumentException e) {
        // Malformed header: use the default language
      }
    }
    return AVAILABLE.get(0);
  }

  public static boolean isRightToLeft(Locale locale) {
    return RTL_LANGUAGES.contains(locale.getLanguage());
  }

  /**
   * @param  locale a locale of {@link #AVAILABLE}
   * @return        the HTML page, translated
   */
  public String page(Locale locale) {
    return pages.computeIfAbsent(locale, this::renderPage);
  }

  /**
   * Returns a static asset of the page. A file of the same name in the
   * configuration directory replaces the default one, so that a distribution
   * can ship its own logo or style (<code>custom.css</code> is loaded after the
   * default style and may change any variable of it). <code>logo-dark.png</code> falls back to the
   * <code>logo.png</code> in use. Only known names are served, nothing else
   * can be read from the disk.
   *
   * @param  name file name requested after /_loading/
   * @return      the asset or null when the name is unknown
   */
  public Asset asset(String name) {
    String type = CONTENT_TYPES.get(name);
    if (type == null) {
      return null;
    }
    if ("theme.css".equals(name)) {
      return new Asset(themeCss().getBytes(StandardCharsets.UTF_8), type);
    }
    byte[] bytes = overridden(name);
    if (bytes.length == 0 && "logo-dark.png".equals(name)) {
      return asset("logo.png");
    }
    if (bytes.length == 0) {
      bytes = classpathBytes("loading/" + name);
    }
    return new Asset(bytes, type);
  }

  private String renderPage(Locale locale) {
    ResourceBundle bundle = bundle(locale);
    ResourceBundle fallback = bundle(AVAILABLE.get(0));
    String page = template;
    for (String key : fallback.keySet()) {
      String value = bundle.containsKey(key) ? bundle.getString(key) : fallback.getString(key);
      page = page.replace("{{" + key + "}}", escape(value));
    }
    return page.replace("{{checkpoints}}", checkpointItems(bundle, fallback))
               .replace("{{lang}}", escape(locale.toLanguageTag()))
               .replace("{{dir}}", isRightToLeft(locale) ? "rtl" : "ltr")
               .replace("{{product}}", escape(branding.getProperty("product.name", "Meeds")));
  }

  private String checkpointItems(ResourceBundle bundle, ResourceBundle fallback) {
    StringBuilder items = new StringBuilder();
    for (Checkpoint checkpoint : checkpoints) {
      String label = label(bundle, fallback, checkpoint.id());
      items.append("<li data-id=\"").append(escape(checkpoint.id())).append("\">").append(escape(label)).append("</li>");
    }
    return items.toString();
  }

  private String themeCss() {
    String primary = color("color.primary", "#3f8487");
    String secondary = color("color.secondary", "#e25d5d");
    String primaryDark = color("color.primary.dark", "#5db3b6");
    String secondaryDark = color("color.secondary.dark", "#ef7f7f");
    return ":root{--primary:" + primary + ";--secondary:" + secondary + "}\n"
        + "@media (prefers-color-scheme:dark){:root{--primary:" + primaryDark + ";--secondary:" + secondaryDark + "}}\n";
  }

  private String color(String key, String defaultValue) {
    String value = branding.getProperty(key);
    return value != null && COLOR.matcher(value.trim()).matches() ? value.trim() : defaultValue;
  }

  private static String label(ResourceBundle bundle, ResourceBundle fallback, String id) {
    String key = "loading.checkpoint." + id;
    if (bundle.containsKey(key)) {
      return bundle.getString(key);
    }
    return fallback.containsKey(key) ? fallback.getString(key) : id;
  }

  private byte[] overridden(String name) {
    return confDir == null ? NO_BYTES : readSmallFile(confDir.resolve(name));
  }

  /**
   * @param  file a file of the configuration directory
   * @return      its content, empty when there is no such file or it can't be
   *              read, so that the default one is used
   */
  private static byte[] readSmallFile(Path file) {
    try {
      return Files.isRegularFile(file) && Files.size(file) <= MAX_ASSET_BYTES ? Files.readAllBytes(file) : NO_BYTES;
    } catch (IOException e) {
      // Unreadable override: the default one is used
      return NO_BYTES;
    }
  }

  private static Properties loadBranding(Path confDir) {
    Properties properties = new Properties();
    byte[] bytes = confDir == null ? NO_BYTES : readSmallFile(confDir.resolve("branding.properties"));
    if (bytes.length > 0) {
      try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
        properties.load(reader);
      } catch (IOException | IllegalArgumentException e) {
        // Unreadable branding: defaults are used
      }
    }
    return properties;
  }

  private static ResourceBundle bundle(Locale locale) {
    try {
      return ResourceBundle.getBundle(BUNDLE_NAME, locale, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    } catch (MissingResourceException e) {
      return ResourceBundle.getBundle(BUNDLE_NAME, AVAILABLE.get(0), ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }
  }

  private static Locale supported(String tag) {
    Locale requested = Locale.forLanguageTag(tag.replace('_', '-'));
    List<Locale> sameLanguage = AVAILABLE.stream().filter(l -> l.getLanguage().equals(requested.getLanguage())).toList();
    return sameLanguage.stream().filter(l -> l.getCountry().equals(requested.getCountry())).findFirst()
                       .orElse(sameLanguage.isEmpty() ? null : sameLanguage.get(0));
  }

  /**
   * Looks for the bundles of the page in every classpath root: the
   * translations added by Crowdin are found without any change of the code.
   *
   * @param  loader the class loader of the application
   * @return        the available languages, the default one first
   */
  static List<Locale> findAvailableLocales(ClassLoader loader) {
    Set<String> tags = new TreeSet<>();
    String prefix = BUNDLE_NAME.substring(0, BUNDLE_NAME.lastIndexOf('.')).replace('.', '/') + "/";
    String fileStart = BUNDLE_NAME.substring(BUNDLE_NAME.lastIndexOf('.') + 1) + "_";
    try {
      // The folder of the bundles, and the default bundle for the jars without folder entries
      List<URL> roots = new ArrayList<>(Collections.list(loader.getResources(prefix)));
      roots.addAll(Collections.list(loader.getResources(prefix + fileStart + "en.properties")));
      for (URL root : roots) {
        if ("file".equals(root.getProtocol())) {
          Path path = Path.of(root.toURI());
          try (var files = Files.list(Files.isDirectory(path) ? path : path.getParent())) {
            files.forEach(file -> addTag(tags, file.getFileName().toString(), fileStart));
          }
        } else if (root.openConnection() instanceof JarURLConnection connection) {
          connection.setUseCaches(false);
          try (JarFile jar = connection.getJarFile()) {
            jar.stream().filter(entry -> entry.getName().startsWith(prefix))
               .forEach(entry -> addTag(tags, entry.getName().substring(prefix.length()), fileStart));
          }
        }
      }
    } catch (IOException | URISyntaxException | RuntimeException e) {
      // Only the default language is available
    }
    List<Locale> locales = new ArrayList<>(List.of(Locale.forLanguageTag("en")));
    tags.stream().map(tag -> Locale.forLanguageTag(tag.replace('_', '-'))).filter(l -> !locales.contains(l)).forEach(locales::add);
    return List.copyOf(locales);
  }

  private static void addTag(Set<String> tags, String fileName, String fileStart) {
    if (fileName.startsWith(fileStart) && fileName.endsWith(".properties")) {
      tags.add(fileName.substring(fileStart.length(), fileName.length() - ".properties".length()));
    }
  }

  private static byte[] classpathBytes(String path) {
    return classpathBytes(LoadingPageContent.class.getClassLoader(), path);
  }

  static byte[] classpathBytes(ClassLoader loader, String path) {
    try (InputStream in = loader.getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException("Missing resource " + path);
      }
      return in.readAllBytes();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read resource " + path, e);
    }
  }

  static String escape(String value) {
    StringBuilder escaped = new StringBuilder(value.length() + ESCAPE_EXTRA);
    for (char c : value.toCharArray()) {
      switch (c) {
      case '&' -> escaped.append("&amp;");
      case '<' -> escaped.append("&lt;");
      case '>' -> escaped.append("&gt;");
      case '"' -> escaped.append("&quot;");
      case '\'' -> escaped.append("&#39;");
      default -> escaped.append(c);
      }
    }
    return escaped.toString();
  }

  /**
   * A static file served by the loading page.
   *
   * @see #asset(String)
   */
  public static final class Asset {

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
}
