package org.motechproject.wa.region.service;

import org.motechproject.wa.region.domain.Circle;
import org.motechproject.wa.region.domain.State;

import java.util.List;
import java.util.Set;

public interface CircleService {
    Circle getByName(String name);
    List<Circle> getAll();
    Set<Circle> getAllInState(final State state);
    boolean circleNameExists(String circleName);
}
