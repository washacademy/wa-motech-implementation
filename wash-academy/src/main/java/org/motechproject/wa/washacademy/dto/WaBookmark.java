package org.motechproject.wa.washacademy.dto;

import java.util.Map;

/**
 * Middle man dto to pass data from API to service layer
 */
public class WaBookmark {

    private Long swcId;

    private String callId;

    private String bookmark;

    private Map<String, Integer> scoresByChapter;

    public WaBookmark() {
    }

    public WaBookmark(Long swcId, String callId, String bookmark, Map<String, Integer> scoresByChapter) {

        this.swcId = swcId;
        this.callId = callId;
        this.bookmark = bookmark;
        this.scoresByChapter = scoresByChapter;
    }

    public Long getSwcId() {
        return swcId;
    }

    public void setSwcId(Long swcId) {
        this.swcId = swcId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getBookmark() {
        return bookmark;
    }

    public void setBookmark(String bookmark) {
        this.bookmark = bookmark;
    }

    public Map<String, Integer> getScoresByChapter() {
        return scoresByChapter;
    }

    public void setScoresByChapter(Map<String, Integer> scoresByChapter) {
        this.scoresByChapter = scoresByChapter;
    }
}
