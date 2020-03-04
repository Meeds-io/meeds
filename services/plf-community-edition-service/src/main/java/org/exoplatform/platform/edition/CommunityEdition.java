package org.exoplatform.platform.edition;

import java.util.*;

import org.exoplatform.container.ExoProfileExtension;

public class CommunityEdition implements ExoProfileExtension {
  @Override
  public Set<String> getProfiles() {
    return Collections.singleton("community");
  }
}
