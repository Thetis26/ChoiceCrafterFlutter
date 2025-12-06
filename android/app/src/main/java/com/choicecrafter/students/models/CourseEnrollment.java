package com.choicecrafter.studentapp.models;

public class CourseEnrollment {
    private String id;
    private String userId;
    private String courseId;
    private String enrollmentDate;
    private String enrolledBy;
    private boolean selfEnrolled;
    private Course course;
    private CourseProgress progress;

    public CourseEnrollment() {
    }

    public CourseEnrollment(String id,
                            String userId,
                            String courseId,
                            String enrollmentDate,
                            String enrolledBy,
                            boolean selfEnrolled) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.enrollmentDate = enrollmentDate;
        this.enrolledBy = enrolledBy;
        this.selfEnrolled = selfEnrolled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(String enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public String getEnrolledBy() {
        return enrolledBy;
    }

    public void setEnrolledBy(String enrolledBy) {
        this.enrolledBy = enrolledBy;
    }

    public boolean isSelfEnrolled() {
        return selfEnrolled;
    }

    public void setSelfEnrolled(boolean selfEnrolled) {
        this.selfEnrolled = selfEnrolled;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public CourseProgress getProgress() {
        return progress;
    }

    public void setProgress(CourseProgress progress) {
        this.progress = progress;
    }

    public String getEnrollmentSourceLabel() {
        if (selfEnrolled) {
            return "Self-enrolled";
        }
        return enrolledBy == null || enrolledBy.isEmpty() ? "Teacher" : "Teacher: " + enrolledBy;
    }
}
