/*
 * Copyright (C) 2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
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
package org.exoplatform.platform.server.tomcat;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.catalina.Engine;
import org.apache.naming.factory.ResourceLinkFactory;
import org.gatein.wci.tomcat.TC7ServletContainerContext;

import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class DataSourceInjector extends BaseContainerLifecyclePlugin {
  private static final Log LOG = ExoLogger.getLogger(DataSourceInjector.class);

  @Override
  public void initContainer(ExoContainer container) throws Exception {
    if (!(container instanceof PortalContainer)) {
      return;
    }
    PortalContainer portalContainer = (PortalContainer) container;
    String portalName = portalContainer.getName();

    if (LOG.isDebugEnabled()) {
      LOG.debug("register portal container classLoader inside " + portalName);
    }

    Context globalNamingContext = getGlobalNamingContext();
    if (globalNamingContext == null) {
      throw new IllegalStateException("Can't access Tomcat Global context");
    }

    List<String> datasourceNames = getListOfDatasources(globalNamingContext);

    // Inject PortalContainer class loader as authorized to access tomcat global
    // datasources
    for (String dsName : datasourceNames) {
      ResourceLinkFactory.registerGlobalResourceAccess(globalNamingContext, dsName, dsName);
    }
  }

  private Context getGlobalNamingContext() {
    // TC7ContainerServlet will start when integration.war context is deployed.
    // The TC7ContainerServlet instantiates the TC7ServletContainerContext.
    // The integration.war is started before the PortalContainer starts
    // So we are sure that the instance TC7ServletContainerContext is instantiated
    // If not, an exception will be thrown in initContainer method and the server startup fails
    TC7ServletContainerContext servletContainerContext = TC7ServletContainerContext.getInstanceIfPresent();
    if (servletContainerContext != null) {
      Engine e = (Engine) servletContainerContext.getEngine();
      return e.getService().getServer().getGlobalNamingContext();
    }
    return null;
  }

  private List<String> getListOfDatasources(Context globalNamingContext) throws NamingException {
    List<String> datasourceNames = new ArrayList<String>();
    NamingEnumeration<NameClassPair> list = globalNamingContext.list("/");
    while (list.hasMore()) {
      NameClassPair next = list.next();
      datasourceNames.add(next.getName());
    }
    return datasourceNames;
  }
}
