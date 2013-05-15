/*
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
package org.exoplatform.platform.server.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.RootContainer;

import java.security.PrivilegedAction;

/**
 *
 */
public class PortalContainersCreator implements LifecycleListener {

  @Override
  public void lifecycleEvent(LifecycleEvent event) {

    // Process the event that has occurred
    if (event.getType().equals(Lifecycle.START_EVENT)) {
      createPortalContainers();
    } else if (event.getType().equals(Lifecycle.STOP_EVENT)) {
      releasePortalContainers();
    }
  }

  /**
   * Initializes and creates all the portal container that have been registered previously
   */
  public void createPortalContainers() {
    final RootContainer rootContainer = RootContainer.getInstance();
    SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>() {
      public Void run() {
        rootContainer.createPortalContainers();
        return null;
      }
    });
  }

  /**
   * Ensure that the root container is stopped properly since the shutdown hook
   * doesn't work in some cases for example with tomcat when we call the stop command
   */
  public void releasePortalContainers() {
    SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>() {
      public Void run() {
        RootContainer.getInstance().stop();
        return null;
      }
    });
  }

}
