package com.jenkins.testresultsaggregator.data;

import java.io.Serializable;

public class HealthReport implements Serializable {
	
	private static final long serialVersionUID = 742123666L;
	
	private int score;
	private String description;
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public int getScore() {
		return score;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
}
