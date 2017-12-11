package org.motechproject.nms.flw.service.impl;

import org.motechproject.nms.flw.service.SwcSettingsService;
import org.motechproject.server.config.SettingsFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * helper service class created to facilitate ITs.
 */
@Service("swcSettingsService")
public class SwcSettingsServiceImpl implements SwcSettingsService {
    private SettingsFacade settingsFacade;

    @Autowired
    SwcSettingsServiceImpl(SettingsFacade settingsFacade) {
        this.settingsFacade = settingsFacade;
    }


    public SettingsFacade getSettingsFacade() {
        return settingsFacade;
    }
}
