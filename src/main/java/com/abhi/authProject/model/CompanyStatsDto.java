package com.abhi.authProject.model;

public class CompanyStatsDto {
    private String companyName;
    private long jobCount;
    private long interviewCount;

    public CompanyStatsDto(String companyName, long jobCount, long interviewCount) {
        this.companyName = companyName;
        this.jobCount = jobCount;
        this.interviewCount = interviewCount;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public long getJobCount() {
        return jobCount;
    }

    public void setJobCount(long jobCount) {
        this.jobCount = jobCount;
    }

    public long getInterviewCount() {
        return interviewCount;
    }

    public void setInterviewCount(long interviewCount) {
        this.interviewCount = interviewCount;
    }
}
