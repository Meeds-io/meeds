package org.exoplatform.platform.server.tomcat;

import java.security.PrivilegedAction;


import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.RootContainer;

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
