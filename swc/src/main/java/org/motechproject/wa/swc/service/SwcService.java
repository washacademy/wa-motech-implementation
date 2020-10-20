package org.motechproject.wa.swc.service;

import org.motechproject.event.MotechEvent;
import org.motechproject.mds.annotations.InstanceLifecycleListener;
import org.motechproject.mds.annotations.InstanceLifecycleListenerType;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.swc.domain.Swachchagrahi;

import java.util.List;

/**
 * Simple example of a service interface.
 */
public interface SwcService {

    State getState(Swachchagrahi swachchagrahi);

    void add(Swachchagrahi swachchagrahi);

    Swachchagrahi getByContactNumber(Long contactNumber);

    Swachchagrahi getByContactNumberAndCourseId(Long contactNumber,Integer courseId);

    Swachchagrahi getInctiveByContactNumber(Long contactNumber);

    Swachchagrahi getInctiveByContactNumberAndCourseId(Long contactNumber,Integer courseId);

    Swachchagrahi getBySwcIdAndCourseId(String swcId,Integer courseId);

    Swachchagrahi getBySwcIdAndPanchayatAndCourseId(String mctsSwcId, Panchayat state, Integer courseId);

    Swachchagrahi getById(Long id);

    List<Swachchagrahi> getRecords();

    void update(Swachchagrahi record);

    void delete(Swachchagrahi record);

    Boolean isAnonymousAllowed(int courseId);

    /**
     * MotechEvent handler that responds to scheduler events.  Purges SWC records that are in invalid state
     * and have been for more than swc.weeks_to_keep_invalid_swcs weeks
     *
     * @param event
     */
    void purgeOldInvalidSWCs(MotechEvent event);

    /**
     * Lifecycle listener that verifies a Front Line Worker can only be deleted if it is invalid
     * and has been in that state for 6 weeks
     *
     * @param swachchagrahi
     */
    @InstanceLifecycleListener(InstanceLifecycleListenerType.PRE_DELETE)
    void deletePreconditionCheck(Swachchagrahi swachchagrahi);
}
