package org.exoplatform.platform.distributions
/**
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import groovy.io.FileType


/**
 * Command line utility to manage Platform extensions in a standalone Apache Tomcat based distribution.
 */

ant = new AntBuilder()

// Computes the script extension from the OS
scriptExtension = "sh"
if (System.properties['os.name'].toLowerCase().contains('windows')) {
  scriptExtension = "bat"
}

def cli = new CliBuilder(
    posix: false,
    stopAtNonOption: true,
    usage: "extension.${scriptExtension} [options]\n",
    header: "eXo Platform Extensions Manager\n\nOptions :\n",
    footer: '\nUse the extension "all" to install or uninstall all available extensions')
// Create the list of options.
cli.with {
  h longOpt: 'help', 'Show usage information'
  l longOpt: 'list', 'List all available extensions'
  i longOpt: 'install', args: 1, argName: 'extension', 'Install an extension'
  u longOpt: 'uninstall', args: 1, argName: 'extension', 'Uninstall an extension'
}

options = cli.parse(args)

// Erroneous command line
if (!options) {
  System.exit 1
}

// Show usage text when -h or --help option is used.
if (options.h) {
  cli.usage()
  System.exit 0
}

// Validate parameters constraints (only one)
if ([options.l, options.i, options.u].findAll { it }.size() != 1) {
  println 'error: Invalid command line parameters. You must use one and only one of these options --list, --install <extension> or --uninstall <extension>'
  cli.usage()
  System.exit 1
}

// Unknown parameter(s)
if (options.arguments()) {
  println "error: Invalid command line parameters. Unknown parameter(s) : ${options.arguments()}"
  cli.usage()
  System.exit 1
}

if (!System.getProperty("catalina.home")) {
  println 'error: Erroneous setup, system property catalina.home not defined.'
  System.exit 1
}

catalinaHome = new File(System.getProperty("catalina.home"))

if (!catalinaHome.isDirectory()) {
  println "error: Erroneous setup, platform home directory (${catalinaHome}) is invalid."
  System.exit 1
}

extensionsDirectory = new File(catalinaHome, "extensions")

if (!extensionsDirectory.isDirectory()) {
  println "error: Erroneous setup, extensions directory (${extensionsDirectory}) is invalid."
  System.exit 1
}

def listExtensions() {
  println ""
  println "Available extensions:"
  extensionsDirectory.eachFile() { file -> println "  ${file.name}" }
  println ""
  println "To install an extension use: "
  println "  extension.${scriptExtension} --install <extension>"
  println ""
  println "To install all avalaible extensions use: "
  println "  extension.${scriptExtension} --install all"
}

def installExtension(String extensionName) {
  def extensionDirectory = new File(extensionsDirectory, extensionName);
  if (!extensionDirectory.isDirectory()) {
    println "error: Extension \"${extensionName}\" doesn't exist."
    listExtensions()
    System.exit 1
  }
  println "Installing ${extensionName} extension ..."
  ant.copy(todir: "${catalinaHome}",
           preservelastmodified: true,
           verbose: true) {
    fileset(dir: "${extensionDirectory}") {
      include(name: "**/*.jar")
      include(name: "**/*.war")
    }
  }
  println "Done."
}

def uninstallExtension(String extensionName) {
  def extensionDirectory = new File(extensionsDirectory, extensionName);
  if (!extensionDirectory.isDirectory()) {
    println "error: Extension \"${extensionName}\" doesn't exist."
    listExtensions()
    System.exit 1
  }
  println "Uninstalling ${extensionName} extension ..."
  extensionDirectory.eachFileRecurse(FileType.FILES) { file ->
    ant.delete(
        file: new File(catalinaHome, extensionDirectory.toURI().relativize(file.toURI()).getPath()),
        verbose: true)
  }
  println "Done."
}

// ListExtensions
if (options.l) {
  listExtensions()
  System.exit 0
}

// InstallExtensions
if (options.i) {
  if ("all".equalsIgnoreCase(options.i))
    extensionsDirectory.eachFile() { file -> installExtension file.name }
  else
    installExtension(options.i)
  System.exit 0
}

// UninstallExtension
if (options.u) {
  if ("all".equalsIgnoreCase(options.u))
    extensionsDirectory.eachFile() { file -> uninstallExtension file.name }
  else
    uninstallExtension(options.u)
  System.exit 0
}