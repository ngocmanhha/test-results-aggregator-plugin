package com.jenkins.testresultsaggregator.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.JobWithDetails;

public class JobWithDetailsAggregator extends JobWithDetails {
	
	private List<HealthReport> healthReport;
	
	// lastBuild,firstBuild,lastCompletedBuild,lastFailedBuild,lastStableBuild,lastSuccessfulBuild,lastUnstableBuild,lastUnsuccessfulBuild
	
	@JsonIgnore
	private Build firstBuild;
	@JsonIgnore
	private Build lastCompletedBuild;
	@JsonIgnore
	private Build lastFailedBuild;
	@JsonIgnore
	private Build lastStableBuild;
	@JsonIgnore
	private Build lastSuccessfulBuild;
	@JsonIgnore
	private Build lastUnstableBuild;
	@JsonIgnore
	private Build lastUnsuccessfulBuild;
	@JsonIgnore
	private Object property;
	@JsonIgnore
	private Object actions;
	
	public void setHealthReport(List<HealthReport> healthReport) {
		this.healthReport = healthReport;
	}
	
	public List<HealthReport> getHealthReport() {
		return healthReport;
	}
	
	public String getHealthReport(boolean icon) {
		if (icon && healthReport != null) {
			for (HealthReport temp : healthReport) {
				if (temp.getDescription().startsWith("Build stability")) {
					return ImagesMap.getImage(temp.getScore());
				}
			}
		}
		return null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((healthReport == null) ? 0 : healthReport.hashCode());
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
		JobWithDetailsAggregator other = (JobWithDetailsAggregator) obj;
		if (healthReport == null) {
			if (other.healthReport != null)
				return false;
		} else if (!healthReport.equals(other.healthReport))
			return false;
		return true;
	}
	
}
