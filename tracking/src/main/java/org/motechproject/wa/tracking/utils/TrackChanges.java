package org.motechproject.wa.tracking.utils;

import java.util.Map;

public interface TrackChanges {

    Map<String, Change> changes();

    Map<String, CollectionChange> collectionChanges();
}
