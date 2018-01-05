package org.motechproject.wa.region.service;

import org.motechproject.wa.region.domain.Circle;
import org.motechproject.wa.region.domain.State;

import java.util.Set;

public interface StateService {
    Set<State> getAllInCircle(final Circle circle);
}
