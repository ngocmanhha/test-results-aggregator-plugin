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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((buildDetails == null) ? 0 : buildDetails.hashCode());
		result = prime * result + ((buildNumber == null) ? 0 : buildNumber.hashCode());
		result = prime * result + ((results == null) ? 0 : results.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BuildWithDetailsAggregator other = (BuildWithDetailsAggregator) obj;
		if (buildDetails == null) {
			if (other.buildDetails != null)
				return false;
		} else if (!buildDetails.equals(other.buildDetails))
			return false;
		if (buildNumber == null) {
			if (other.buildNumber != null)
				return false;
		} else if (!buildNumber.equals(other.buildNumber))
			return false;
		if (results == null) {
			if (other.results != null)
				return false;
		} else if (!results.equals(other.results))
			return false;
		return true;
	}
	
}
