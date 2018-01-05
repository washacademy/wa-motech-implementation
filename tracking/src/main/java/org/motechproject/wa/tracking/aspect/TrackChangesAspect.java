package org.motechproject.wa.tracking.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareMixin;
import org.aspectj.lang.annotation.Pointcut;
import org.motechproject.mds.util.PropertyUtil;
import org.motechproject.wa.tracking.exception.TrackChangesException;
import org.motechproject.wa.tracking.service.TrackChangesService;
import org.motechproject.wa.tracking.utils.TrackChangeUtils;
import org.motechproject.wa.tracking.utils.TrackChanges;
import org.motechproject.wa.tracking.utils.TrackChangesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

@Aspect
public class TrackChangesAspect {

    public static final Logger LOGGER = LoggerFactory.getLogger(TrackChangesAspect.class);

    private TrackChangesService trackChangesService;

    @DeclareMixin("(@TrackClass *)")
    public static TrackChanges implementTrackChanges() {
        return new TrackChangesImpl();
    }

    @Pointcut("staticinitialization(@TrackClass *)")
    public void trackedClassInit() { }

    @Pointcut("within(@TrackClass *)")
    public void withinTrackClass() { }

    @Pointcut("within(@TrackFields *)")
    public void withinTrackFields() { }

    @Pointcut("set(@TrackField * *)")
    public void trackedFieldWithAnnotationSetter() { }

    @Pointcut("set(* *) && withinTrackFields()")
    public void trackedFieldWithinAnnotationSetter() { }

    @Pointcut("trackedFieldWithAnnotationSetter() || trackedFieldWithinAnnotationSetter()")
    public void trackedFieldSetter() { }

    @Pointcut("get(@TrackField java.util.Collection+ *)")
    public void trackedCollectionFieldWithAnnotationGetter() { }

    @Pointcut("get(java.util.Collection+ *) && withinTrackFields()")
    public void trackedCollectionFieldWithinAnnotationGetter() { }

    @Pointcut("trackedCollectionFieldWithAnnotationGetter() || trackedCollectionFieldWithinAnnotationGetter()")
    public void trackedCollectionFieldGetter() { }

    @After("trackedClassInit()")
    public void registerJdoLifecycleListeners(JoinPoint.StaticPart staticPart) {
        if (null != trackChangesService) {
            try {
                trackChangesService.registerLifecycleListeners(staticPart.getSignature().getDeclaringType());
            } catch (Exception e) {
                // log and rethrow static block exceptions
                LOGGER.error("Failed to register change listeners", e);
                throw e;
            }
        } else {
            LOGGER.warn("The %s service is missing. This aspect either failed to initialize as a bean " +
                    "or is used outside the application context", TrackChangesService.class.getSimpleName());
        }
    }

    @Before("withinTrackClass() && trackedFieldSetter()")
    public void beforeTrackedFieldSetter(JoinPoint joinPoint) {
        try {
            TrackChanges target = (TrackChanges) joinPoint.getTarget();
            String property = getProperty(joinPoint);
            Object oldValue = getOldValue(joinPoint);
            Object newValue = getNewValue(joinPoint);
            TrackChangeUtils.trackChange(target, property, oldValue, newValue);
        } catch (TrackChangesException e) {
            LOGGER.error("Unable to track field changes", e);
        }
    }

    @Around("withinTrackClass() && trackedCollectionFieldGetter()")
    public Collection aroundTrackedCollectionFieldGetter(ProceedingJoinPoint joinPoint) throws Throwable { //NO CHECKSTYLE Throwing 'Throwable' is not allowed.
        TrackChanges target = (TrackChanges) joinPoint.getTarget();
        String property = getProperty(joinPoint);
        Collection collection = (Collection) joinPoint.proceed();
        return TrackChangeUtils.decorateTrackedCollection(target, property, collection);
    }

    private Object getOldValue(JoinPoint joinPoint) throws TrackChangesException {
        try {
            return PropertyUtil.getProperty(joinPoint.getTarget(), getProperty(joinPoint));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new TrackChangesException("Unable to retrieve old value", e);
        }
    }

    private Object getNewValue(JoinPoint joinPoint) throws TrackChangesException {
        if (joinPoint.getArgs().length == 1) {
            return joinPoint.getArgs()[0];
        } else {
            throw new TrackChangesException("Unable to retrieve new value");
        }
    }

    private String getProperty(JoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }

    @Autowired
    public void setTrackChangesService(TrackChangesService trackChangesService) {
        this.trackChangesService = trackChangesService;
    }

}
