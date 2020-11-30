package org.motechproject.wa.api.web.contract.washAcademy;

import java.util.Map;

public class UserIDBookmarkAndCourseResponse {
    private String userId;
    private Long contactNumber;
    private String courseIdentifier;
    private String bookmark;
    private Map<String, Integer> scoresByChapter;

    public UserIDBookmarkAndCourseResponse() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(Long contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getCourseIdentifier() {
        return courseIdentifier;
    }

    public void setCourseIdentifier(String courseIdentifier) {
        this.courseIdentifier = courseIdentifier;
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
