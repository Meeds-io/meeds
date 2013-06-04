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
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

/**
 * Command line utility to manage Platform extensions in a standalone Apache Tomcat based distribution.
 */

println """
 # ===============================
 # eXo Platform Extensions Manager
 # ===============================
"""

ant = new AntBuilder()

scriptBaseName = "extension"
// Computes the script extension from the OS
scriptName = "${scriptBaseName}.sh"
if (System.properties['os.name'].toLowerCase().contains('windows')) {
  scriptName = "${scriptBaseName}.bat"
}

def cli = new CliBuilder(
    posix: false,
    stopAtNonOption: true,
    usage: """
${scriptName} --list
${scriptName} --install <extension>
${scriptName} --uninstall <extension>
""",
    header: "Options :",
    footer: """

Use the extension "all" to install or uninstall all available extensions

""")

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
if (args.length == 0 || options.h) {
  cli.usage()
  System.exit 0
}

// Unknown parameter(s)
// And validate parameters constraints (only one)
if (options.arguments() || [options.l, options.i, options.u].findAll { it }.size() != 1) {
  println "error: Invalid command line parameter(s)."
  cli.usage()
  System.exit 1
}

if (!System.getProperty("product.home")) {
  println 'error: Erroneous setup, system property product.home not defined.'
  System.exit 1
}

productHome = new File(System.getProperty("product.home"))

if (!productHome.isDirectory()) {
  println "error: Erroneous setup, product home directory (${productHome}) is invalid."
  System.exit 1
}

extensionsDirectory = new File(productHome, "extensions")

if (!extensionsDirectory.isDirectory()) {
  println "error: Erroneous setup, extensions directory (${extensionsDirectory}) is invalid."
  System.exit 1
}

if (!System.getProperty("platform.libraries.path")) {
  println 'error: Erroneous setup, system property platform.libraries.path not defined.'
  System.exit 1
}

librariesDir = new File(productHome, System.getProperty("platform.libraries.path"))

if (!librariesDir.isDirectory()) {
  println "error: Erroneous setup, platform libraries directory (${librariesDir}) is invalid."
  System.exit 1
}

if (!System.getProperty("platform.webapps.path")) {
  println 'error: Erroneous setup, system property platform.webapps.path not defined.'
  System.exit 1
}

webappsDir = new File(productHome, System.getProperty("platform.webapps.path"))

if (!webappsDir.isDirectory()) {
  println "error: Erroneous setup, platform web applications directory (${webappsDir}) is invalid."
  System.exit 1
}

def listExtensions() {
  println "Available extensions:"
  extensionsDirectory.eachDir() { dir -> println "  ${dir.name}" }
  println """
To install an extension use:
  ${scriptName} --install <extension>

To install all avalaible extensions use:
  ${scriptName} --install all
"""
}

def String serializeXml(GPathResult xml) {
  XmlUtil.serialize(new StreamingMarkupBuilder().bind {
    mkp.yield xml
  })
}

def processFileInplace(file, Closure processText) {
  def text = file.text
  file.write(processText(text))
}

def installExtension(String extensionName) {
  def extensionDirectory = new File(extensionsDirectory, extensionName);
  def extensionLibDirectory = new File(extensionDirectory, "lib");
  def extensionWebappDirectory = new File(extensionDirectory, "webapps");
  if (!extensionDirectory.isDirectory()) {
    println "error: Extension \"${extensionName}\" doesn't exist."
    listExtensions()
    System.exit 1
  }
  println "Installing ${extensionName} extension ..."
  if (extensionLibDirectory.isDirectory()) {
    ant.copy(todir: "${librariesDir}",
        preservelastmodified: true,
        verbose: true) {
      fileset(dir: "${extensionLibDirectory}") {
        include(name: "*.jar")
      }
    }
  }
  if (extensionWebappDirectory.isDirectory()) {
    ant.copy(todir: "${webappsDir}",
        preservelastmodified: true,
        verbose: true) {
      fileset(dir: "${extensionWebappDirectory}") {
        include(name: "*.war")
      }
    }
    // Update application.xml if it exists
    def applicationDescriptor = new File(webappsDir, "META-INF/application.xml")
    if (applicationDescriptor.exists()) {
      processFileInplace(applicationDescriptor) { text ->
        application = new XmlSlurper(false, false).parseText(text)
        extensionWebappDirectory.eachFileRecurse(FileType.FILES) { file ->
          def webArchive = file.name
          def webContext = file.name.substring(0, file.name.length() - 4)
          print "Adding/Updating context declaration /${webContext} for ${webArchive} in application.xml ... "
          application.depthFirst().findAll { (it.name() == 'module') && (it.'web'.'web-uri'.text() == webArchive) }.each { node ->
            // remove existing node
            node.replaceNode {}
          }
          application."initialize-in-order" + {
            module {
              web {
                'web-uri'(webArchive)
                'context-root'(webContext)
              }
            }
          }
          println "OK"
        }
        serializeXml(application)
      }
    }
  }
  println "Done."
}

def uninstallExtension(String extensionName) {
  def extensionDirectory = new File(extensionsDirectory, extensionName);
  def extensionLibDirectory = new File(extensionDirectory, "lib");
  def extensionWebappDirectory = new File(extensionDirectory, "webapps");
  if (!extensionDirectory.isDirectory()) {
    println "error: Extension \"${extensionName}\" doesn't exist."
    listExtensions()
    System.exit 1
  }
  println "Uninstalling ${extensionName} extension ..."
  if (extensionLibDirectory.isDirectory()) {
    extensionLibDirectory.eachFileRecurse(FileType.FILES) { file ->
      ant.delete(
          file: new File(librariesDir, file.name))
    }
  }
  if (extensionWebappDirectory.isDirectory()) {
    // Update application.xml if it exists
    def applicationDescriptor = new File(webappsDir, "META-INF/application.xml")
    extensionWebappDirectory.eachFileRecurse(FileType.FILES) { file ->
      ant.delete(
          file: new File(webappsDir, file.name))
      if (applicationDescriptor.exists()) {
        processFileInplace(applicationDescriptor) { text ->
          application = new XmlSlurper(false, false).parseText(text)
          def webArchive = file.name
          def webContext = file.name.substring(0, file.name.length() - 4)
          application.depthFirst().findAll { (it.name() == 'module') && (it.'web'.'web-uri'.text() == webArchive) }.each { node ->
            print "Removing context declaration /${webContext} for ${webArchive} in application.xml ... "
            // remove existing node
            node.replaceNode {}
            println "OK"
          }
          serializeXml(application)
        }
      }
    }
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
  if ("all".equalsIgnoreCase(options.i)) {
    extensionsDirectory.eachDir() { dir -> installExtension dir.name }
    println """
 # ===============================
 # All extensions installed.
 # ===============================
"""
  } else {
    installExtension(options.i)
    println """
 # ===============================
 # Extension ${options.i} installed.
 # ===============================
"""
  }
  System.exit 0
}

// UninstallExtension
if (options.u) {
  if ("all".equalsIgnoreCase(options.u)) {
    extensionsDirectory.eachDir() { dir -> uninstallExtension dir.name }
    println """
 # ==============================
 # All extensions uninstalled.
 # ===============================
"""
  } else {
    uninstallExtension(options.u)
    println """
 # ===============================
 # Extension ${options.u} uninstalled.
 # ===============================
"""
  }
  System.exit 0
}