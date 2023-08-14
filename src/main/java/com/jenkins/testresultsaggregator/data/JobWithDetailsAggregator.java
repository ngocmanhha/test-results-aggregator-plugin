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
}
