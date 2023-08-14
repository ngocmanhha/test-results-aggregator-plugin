package com.jenkins.testresultsaggregator.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.common.base.Strings;
import com.jenkins.testresultsaggregator.helper.Colors;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class Job extends AbstractDescribableImpl<Job> implements Serializable {
	
	private static final long serialVersionUID = 34911974223666L;
	
	private String jobName;
	private String jobFriendlyName;
	private String url;
	private String folder;
	private boolean isBuilding;
	private int buildNumber;
	private JobStatus jobStatus;
	// Job
	private JobWithDetails jobDetails;
	// Last Build
	private Integer lastBuildNumber;
	private BuildWithDetails lastBuildDetails;
	private Results lastBuildResults;
	// Previous
	private Integer previousBuildNumber;
	private BuildWithDetails previousBuildDetails;
	private Results previousBuildResults;
	// Report
	private ReportJob report;
	
	@Extension
	public static class JobDescriptor extends Descriptor<Job> {
		@Override
		public String getDisplayName() {
			return "";
		}
		
	}
	
	@DataBoundConstructor
	public Job() {
		
	}
	
	public Job(String jobName, String jobFriendlyName) {
		setJobName(jobName);
		setJobFriendlyName(jobFriendlyName);
	}
	
	public String getJobName() {
		if (jobName != null) {
			return jobName.trim();
		}
		return jobName;
	}
	
	@DataBoundSetter
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	public String getJobFriendlyName() {
		if (jobFriendlyName != null) {
			return jobFriendlyName.trim();
		}
		return jobFriendlyName;
	}
	
	@DataBoundSetter
	public void setJobFriendlyName(String jonFriendlyName) {
		this.jobFriendlyName = jonFriendlyName;
	}
	
	public BuildWithDetails getLastBuildDetails() {
		return lastBuildDetails;
	}
	
	public void setLastBuildDetails(BuildWithDetails lastBuildDetails) {
		this.lastBuildDetails = lastBuildDetails;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getFolder() {
		return folder;
	}
	
	public void setFolder(String folder) {
		this.folder = folder;
	}
	
	public BuildWithDetails getPreviousBuildDetails() {
		return previousBuildDetails;
	}
	
	public void setPreviousBuildDetails(BuildWithDetails previousBuildDetails) {
		this.previousBuildDetails = previousBuildDetails;
	}
	
	public Results getLastBuildResults() {
		return lastBuildResults;
	}
	
	public void setLastBuildResults(Results lastBuildResults) {
		this.lastBuildResults = lastBuildResults;
	}
	
	public JobWithDetails getJobDetails() {
		return jobDetails;
	}
	
	public void setJobDetails(JobWithDetails jobDetails) {
		this.jobDetails = jobDetails;
	}
	
	public ReportJob getReport() {
		return report;
	}
	
	public void setReport(ReportJob report) {
		this.report = report;
	}
	
	public String getJobNameFromFriendlyName() {
		if (Strings.isNullOrEmpty(jobFriendlyName)) {
			return jobName;
		}
		return jobFriendlyName;
	}
	
	public String getJobNameFromFriendlyName(boolean withLinktoResults) {
		if (withLinktoResults) {
			String reportUrl = null;
			if (lastBuildResults == null) {
				reportUrl = null;
				// Get job and execution id
			} else if (Strings.isNullOrEmpty(lastBuildResults.getUrl())) {
				reportUrl = null;
			} else {
				reportUrl = lastBuildResults.getUrl();
			}
			// iF this is still null Use Job url
			if (Strings.isNullOrEmpty(reportUrl)) {
				reportUrl = url;
			}
			return "<a href='" + reportUrl + "'><font color='" + Colors.htmlJOB_NAME_URL() + "'>" + getJobNameFromFriendlyName() + "</font></a>";
		}
		return getJobNameFromFriendlyName();
	}
	
	public Integer getPreviousBuildNumber() {
		return previousBuildNumber;
	}
	
	public void setPreviousBuildNumber(Integer previousBuildNumber) {
		this.previousBuildNumber = previousBuildNumber;
	}
	
	public Integer getLastBuildNumber() {
		return lastBuildNumber;
	}
	
	public void setLastBuildNumber(Integer lastBuildNumber) {
		this.lastBuildNumber = lastBuildNumber;
	}
	
	public Results getPreviousBuildResults() {
		return previousBuildResults;
	}
	
	public void setPreviousBuildResults(Results previousBuildResults) {
		this.previousBuildResults = previousBuildResults;
	}
	
	public boolean getIsBuilding() {
		return isBuilding;
	}
	
	public void setIsBuilding(boolean isBuilding) {
		this.isBuilding = isBuilding;
	}
	
	public int getBuildNumber() {
		return buildNumber;
	}
	
	public void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}
	
	public String getBuildNumberUrl() {
		return "<a href='" + url + "/" + buildNumber + "'>" + buildNumber + "</a>";
	}
	
	public JobStatus getJobStatus() {
		return jobStatus;
	}
	
	public void setJobStatus(JobStatus jobStatus) {
		this.jobStatus = jobStatus;
	}
	
	private void writeObject(ObjectOutputStream stream)
			throws IOException {
		stream.defaultWriteObject();
	}
	
	private void readObject(ObjectInputStream stream)
			throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
	}
}
