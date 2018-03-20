package org.motechproject.wa.imi.service.impl;

import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.imi.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * helper service class created to, yep, help change OBD configuration to facilitate ITs.
 */
@Service("settingsService")
public class SettingsServiceImpl implements SettingsService {
    private SettingsFacade settingsFacade;

    @Autowired
    SettingsServiceImpl(SettingsFacade settingsFacade) {
        this.settingsFacade = settingsFacade;
    }


    public SettingsFacade getSettingsFacade() {
        return settingsFacade;
    }
}
