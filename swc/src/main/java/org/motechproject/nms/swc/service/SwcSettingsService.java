package org.motechproject.nms.swc.service;

import org.motechproject.server.config.SettingsFacade;

/**
 * Gives access to swc.properties to ITs
 */
public interface SwcSettingsService {
    SettingsFacade getSettingsFacade();
}
