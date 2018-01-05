package org.motechproject.wa.swc.domain;

/**
 * Created by vishnu on 26/12/17.
 */
public enum DeactivationReason {
    DEACTIVATED_BY_USER, LIVE_BIRTH, MISCARRIAGE_OR_ABORTION, STILL_BIRTH, CHILD_DEATH, MATERNAL_DEATH, INVALID_NUMBER, DO_NOT_DISTURB, MCTS_UPDATE, WEEKLY_CALLS_NOT_ANSWERED, LOW_LISTENERSHIP;

    private DeactivationReason() { /* compiled code */ }
}
