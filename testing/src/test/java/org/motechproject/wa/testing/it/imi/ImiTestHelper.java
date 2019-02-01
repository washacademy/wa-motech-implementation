package org.motechproject.wa.testing.it.imi;

import org.motechproject.wa.imi.service.SettingsService;

import java.io.File;

/**
 * Common setup and helper methods used by IMI ITs
 */
public final class ImiTestHelper {

    protected static final String ADMIN_USERNAME = "motech";
    protected static final String ADMIN_PASSWORD = "motech";

    public static String setupTestDir(SettingsService settingsService, String property, String dir) {
        String backup = settingsService.getSettingsFacade().getProperty(property);
        dir = ".motech/" + dir;
        File directory = new File(System.getProperty("user.home"), dir);
        directory.mkdirs();
        settingsService.getSettingsFacade().setProperty(property, directory.getAbsolutePath());
        return backup;
    }
}
