package com.jenkins.testresultsaggregator.data;

import java.util.List;

import com.offbytwo.jenkins.model.JobWithDetails;

public class JobWithDetailsAggregator extends JobWithDetails {
	
	private List<HealthReport> healthReport;
	
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
