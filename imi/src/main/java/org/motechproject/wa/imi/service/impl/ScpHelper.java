package org.motechproject.wa.imi.service.impl;


import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.imi.exception.ExecException;


/**
 * Uses the operating system to scp & sort CDR files and scp OBD files
 */
@SuppressWarnings("PMD")
public class ScpHelper {
//
//    private static final String SCP_FROM_COMMAND = "imi.scp.from_command";
//    private static final String SCP_FROM_COMMAND_DEFAULT = "/bin/cp {src} {dst}";
//    private static final String SCP_TO_COMMAND = "imi.scp.to_command";
//    private static final String SCP_TO_COMMAND_DEFAULT = "/bin/cp {src} {dst}";
//    private static final String SCP_TIMEOUT_SETTING = "imi.scp.timeout";
//    private static final Long SCP_TIMEOUT_DEFAULT = 60000L;
//
//    private SettingsFacade settingsFacade;

//
//    public ScpHelper(SettingsFacade settingsFacade) {
//        this.settingsFacade = settingsFacade;
//    }
//
//
//    private String getSettingWithDefault(String setting, String defaultValue) {
//        String s = settingsFacade.getProperty(setting);
//        if (s == null || s.isEmpty()) {
//            return  defaultValue;
//        }
//        return s;
//    }

//
//    private String scpFromCommand(String src, String dst) {
//        String command = getSettingWithDefault(SCP_FROM_COMMAND, SCP_FROM_COMMAND_DEFAULT);
//
//        return command.replace("{src}", src).replace("{dst}", dst);
//    }
//
//
//    private String scpToCommand(String src, String dst) {
//        String command = getSettingWithDefault(SCP_TO_COMMAND, SCP_TO_COMMAND_DEFAULT);
//
//        return command.replace("{src}", src).replace("{dst}", dst);
//    }
//
//
//    private Long getScpTimeout() {
//        try {
//            return Long.parseLong(settingsFacade.getProperty(SCP_TIMEOUT_SETTING));
//        } catch (NumberFormatException e) {
//            return SCP_TIMEOUT_DEFAULT;
//        }
//    }

}
