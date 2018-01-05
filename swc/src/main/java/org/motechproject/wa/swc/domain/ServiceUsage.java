package org.motechproject.wa.swc.domain;

import org.motechproject.wa.props.domain.Service;

public class ServiceUsage {

    private Swachchagrahi swachchagrahi;

    private Service service;

    private int usageInPulses;

    private int endOfUsage;

    private boolean welcomePrompt;


    public ServiceUsage(Swachchagrahi swachchagrahi, Service service, int usageInPulses, int endOfUsage,
                        boolean welcomePrompt) {
        this.swachchagrahi = swachchagrahi;
        this.service = service;
        this.usageInPulses = usageInPulses;
        this.endOfUsage = endOfUsage;
        this.welcomePrompt = welcomePrompt;
    }

    public Swachchagrahi getSwachchagrahi() {
        return swachchagrahi;
    }

    public void setSwachchagrahi(Swachchagrahi swachchagrahi) {
        this.swachchagrahi = swachchagrahi;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public int getUsageInPulses() {
        return usageInPulses;
    }

    public void setUsageInPulses(int usageInPulses) {
        this.usageInPulses = usageInPulses;
    }

    public int getEndOfUsage() {
        return endOfUsage;
    }

    public void setEndOfUsage(int endOfUsage) {
        this.endOfUsage = endOfUsage;
    }

    public boolean getWelcomePrompt() {
        return welcomePrompt;
    }

    public void setWelcomePrompt(boolean welcomePrompt) {
        this.welcomePrompt = welcomePrompt;
    }
}
