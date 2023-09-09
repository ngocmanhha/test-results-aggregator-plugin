package com.jenkins.testresultsaggregator.data;

import com.offbytwo.jenkins.model.BuildWithDetails;

public class BuildWithDetailsAggregator extends BuildWithDetails {
	
	private Integer buildNumber;
	private BuildWithDetails buildDetails;
	private JobResults results;
	
	public BuildWithDetailsAggregator() {
		
	}
	
	public Integer getBuildNumber() {
		return buildNumber;
	}
	
	public void setBuildNumber(Integer buildNumber) {
		this.buildNumber = buildNumber;
	}
	
	public BuildWithDetails getBuildDetails() {
		return buildDetails;
	}
	
	public void setBuildDetails(BuildWithDetails buildDetails) {
		this.buildDetails = buildDetails;
	}
	
	public JobResults getResults() {
		return results;
	}
	
	public void setResults(JobResults results) {
		this.results = results;
	}
	
}
