package com.jenkins.testresultsaggregator.actions;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jenkins.testresultsaggregator.data.BuildWithDetailsAggregator;
import com.jenkins.testresultsaggregator.data.Data;
import com.jenkins.testresultsaggregator.data.Job;
import com.jenkins.testresultsaggregator.data.JobResults;
import com.jenkins.testresultsaggregator.data.JobStatus;
import com.jenkins.testresultsaggregator.data.JobWithDetailsAggregator;
import com.jenkins.testresultsaggregator.data.Results;
import com.jenkins.testresultsaggregator.helper.Helper;
import com.jenkins.testresultsaggregator.helper.LocalMessages;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpConnection;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;

import hudson.util.Secret;

public class Collector {
	
	public static final String ROOT_FOLDER = "root";
	
	// Actions
	public static final String CHANGES = "changes";
	public static final String FAILCOUNT = "failCount";
	public static final String SKIPCOUNT = "skipCount";
	public static final String TOTALCOUNT = "totalCount";
	public static final String JACOCO_BRANCH = "branchCoverage";
	public static final String JACOCO_CLASS = "classCoverage";
	public static final String JACOCO_LINES = "lineCoverage";
	public static final String JACOCO_METHODS = "methodCoverage";
	public static final String JACOCO_INSTRUCTION = "instructionCoverage";
	public static final String SONAR_URL = "sonarqubeDashboardUrl";
	// Urls
	public static final String DEPTH = "?depth=1";
	public static final String COBERTURA = "cobertura/" + "api/json" + "?depth=2";
	
	private PrintStream logger;
	JenkinsServer jenkins;
	Map<String, com.offbytwo.jenkins.model.Job> jobs;
	
	public Collector(String jenkinsUrl, String username, Secret password, PrintStream printStream) throws URISyntaxException, IOException {
		this.jenkins = new JenkinsServer(new URI(jenkinsUrl), username, password.getPlainText());
		this.jobs = jenkins.getJobs();
		this.logger = printStream;
	}
	
	public void closeJenkinsConnection() {
		if (jenkins != null) {
			jenkins.close();
		}
	}
	
	public JobWithDetailsAggregator getDetails(Job job) throws IOException {
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			if (jobs.get(job.getJobNameOnly()) != null) {
				return jobs.get(job.getJobNameOnly()).getClient().get(jobs.get(job.getJobNameOnly()).details().getUrl() + DEPTH, JobWithDetailsAggregator.class);
			}
		} else {
			JenkinsHttpConnection client = jenkins.getQueue().getClient();
			if (client != null) {
				return client.get(job.getUrl() + DEPTH, JobWithDetailsAggregator.class);
			}
		}
		return null;
	}
	
	public BuildWithDetails getLastBuildDetails(Job job) throws IOException {
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			if (jobs.get(job.getJobNameOnly()) != null) {
				return jobs.get(job.getJobNameOnly()).getClient().get(jobs.get(job.getJobNameOnly()).details().getLastBuild().details().getUrl() + DEPTH, BuildWithDetails.class);
			}
		} else {
			JenkinsHttpConnection client = jenkins.getQueue().getClient();
			if (client != null) {
				return client.get(job.getUrl() + DEPTH, BuildWithDetails.class);
			}
		}
		return null;
	}
	
	public BuildWithDetails getBuildDetails(Job job, Integer number) throws IOException {
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			if (jobs.get(job.getJobNameOnly()) != null && number != null) {
				return jobs.get(job.getJobNameOnly()).getClient().get(jobs.get(job.getJobNameOnly()).details().getBuildByNumber(number).details().getUrl() + DEPTH, BuildWithDetails.class);
			} else {
				JenkinsHttpConnection client = jenkins.getQueue().getClient();
				if (client != null) {
					return client.get(job.getUrl() + DEPTH, BuildWithDetails.class);
				}
			}
		}
		return null;
	}
	
	public void collectResults(List<Data> dataJob, boolean compareWithPreviousRun, Boolean ignoreRunningJobs) throws InterruptedException {
		List<Job> allDataJobDTO = new ArrayList<>();
		for (Data temp : dataJob) {
			if (temp.getJobs() != null && !temp.getJobs().isEmpty()) {
				allDataJobDTO.addAll(temp.getJobs());
			}
		}
		ReportThread[] threads = new ReportThread[allDataJobDTO.size()];
		int index = 0;
		for (Job tempDataJobDTO : allDataJobDTO) {
			threads[index] = new ReportThread(tempDataJobDTO, compareWithPreviousRun, ignoreRunningJobs);
			index++;
		}
		index = 0;
		for (ReportThread thread : threads) {
			thread.start();
			index++;
			if (index % 6 == 0) {
				Thread.sleep(4000);
			}
		}
		for (ReportThread thread : threads) {
			thread.join(60000);
		}
	}
	
	public class ReportThread extends Thread {
		
		Job job;
		boolean compareWithPreviousRun;
		boolean ignoreRunningJobs;
		
		public ReportThread(Job job, boolean compareWithPreviousRun, boolean ignoreRunningJobs) {
			this.job = job;
			this.compareWithPreviousRun = compareWithPreviousRun;
			this.ignoreRunningJobs = ignoreRunningJobs;
		}
		
		@Override
		public void run() {
			try {
				job.setJob(getDetails(job));
				if (job.getJob() == null) {
					logger.println("Job '" + job.getJobName() + "' found " + JobStatus.NOT_FOUND.name());
					job.setResults(new Results(JobStatus.NOT_FOUND.name(), job.getUrl()));
				} else if (!job.getJob().isBuildable()) {
					logger.println("Job '" + job.getJobName() + "' found " + JobStatus.DISABLED.name());
					job.setResults(new Results(JobStatus.DISABLED.name(), job.getUrl()));
				} else if (job.getJob().isBuildable() && !job.getJob().hasLastBuildRun()) {
					logger.println("Job '" + job.getJobName() + "' found " + JobStatus.NO_LAST_BUILD_DATA.name());
					job.setResults(new Results(JobStatus.NO_LAST_BUILD_DATA.name(), job.getUrl()));
				} else {
					// Job FOUND
					job.setLast(new BuildWithDetailsAggregator());
					job.getLast().setBuildDetails(getLastBuildDetails(job));
					job.getLast().setResults(new JobResults().calculatedFrom(job.getLast().getBuildDetails()));
					job.setIsBuilding(job.getLast().getBuildDetails().isBuilding());
					logger.print("Job '" + job.getJobName() + "' found build number " + job.getLast().getBuildDetails().getNumber() + " with status");
					if (job.getLast().getBuildDetails().isBuilding()) {
						logger.println(" building");
						if (ignoreRunningJobs) {
							Integer previousBuildNumber = null;
							if (job.getResults() == null) {
								job.setResults(new Results(JobStatus.RUNNING_REPORT_PREVIOUS.name(), job.getUrl()));
								previousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
							} else {
								job.getResults().setStatus(JobStatus.RUNNING_REPORT_PREVIOUS.name());
								job.getResults().setUrl(job.getUrl());
								previousBuildNumber = job.getResults().getNumber();
							}
							job.setLast(new BuildWithDetailsAggregator());
							job.getLast().setBuildNumber(previousBuildNumber);
							job.getLast().setBuildDetails(getBuildDetails(job, previousBuildNumber));
							job.getLast().setResults(new JobResults().calculatedFrom(job.getLast().getBuildDetails()));
							//
							job.setPrevious(new BuildWithDetailsAggregator());
							job.getPrevious().setBuildNumber(previousBuildNumber);
							job.getPrevious().setBuildDetails(job.getLast().getBuildDetails());
							job.getPrevious().setResults(job.getLast().getResults());
						} else {
							job.setResults(new Results(JobStatus.RUNNING.name(), job.getUrl()));
						}
					} else {
						logger.println(" " + job.getLast().getBuildDetails().getResult().toString().toLowerCase());
						if (compareWithPreviousRun) {
							Integer previousBuildNumber = null;
							if (job.getResults() == null) {
								// Not Found previously saved , resolve from jenkins
								job.setResults(new Results(JobStatus.FOUND.name(), job.getUrl()));
								previousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
							} else {
								// Found previously saved use them
								previousBuildNumber = job.getResults().getNumber();
							}
							if (previousBuildNumber == job.getLast().getBuildDetails().getNumber()) {
								// There is no new run since the previous aggregator run
								job.setLast(new BuildWithDetailsAggregator());
								job.getLast().setBuildNumber(previousBuildNumber);
								job.getLast().setBuildDetails(getBuildDetails(job, previousBuildNumber));
								job.getLast().setResults(new JobResults().calculatedFrom(job.getLast().getBuildDetails()));
								//
								Integer previousOfPreviousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
								job.setPrevious(new BuildWithDetailsAggregator());
								job.getPrevious().setBuildNumber(previousOfPreviousBuildNumber);
								job.getPrevious().setBuildDetails(getBuildDetails(job, previousOfPreviousBuildNumber));
								job.getPrevious().setResults(new JobResults().calculatedFrom(job.getPrevious().getBuildDetails()));
							} else {
								job.setPrevious(new BuildWithDetailsAggregator());
								job.getPrevious().setBuildNumber(previousBuildNumber);
								job.getPrevious().setBuildDetails(getBuildDetails(job, previousBuildNumber));
								job.getPrevious().setResults(new JobResults().calculatedFrom(job.getPrevious().getBuildDetails()));
							}
						}
					}
					logger.println(LocalMessages.COLLECT_DATA.toString() + " '" + job.getJobName() + "' " + LocalMessages.FINISHED.toString());
				}
			} catch (IOException e) {
				logger.println("Job '" + job.getJobName() + "' found " + JobStatus.NOT_FOUND.name() + "with error : " + e.getMessage());
				job.setResults(new Results(JobStatus.NOT_FOUND.name(), job.getUrl()));
				e.printStackTrace();
			}
		}
	}
	
	private Integer resolvePreviousBuildNumberFromBuild(Job job, int depth) {
		try {
			List<Integer> allBuildNumbers = job.getJob().getAllBuilds().stream().map(Build::getNumber).collect(Collectors.toList());
			Collections.sort(allBuildNumbers);
			return allBuildNumbers.get(allBuildNumbers.size() - depth);
		} catch (Exception ex) {
			// ex.printStackTrace();
		}
		return null;
	}
	
	private void actions(BuildWithDetails buildWithDetail, Results results) {
		List<?> actionList = buildWithDetail.getActions();
		for (Object temp : actionList) {
			HashMap<Object, Object> actions = (HashMap<Object, Object>) temp;
			if (actions.containsKey("_class") && !actions.get("_class").equals("com.jenkins.testresultsaggregator.TestResultsAggregatorTestResultBuildAction")) {
				// Calculate FAIL,SKIP and TOTAL Test Results
				if (actions.containsKey(FAILCOUNT)) {
					results.setFail((Integer) actions.get(FAILCOUNT));
				}
				if (actions.containsKey(SKIPCOUNT)) {
					results.setSkip((Integer) actions.get(SKIPCOUNT));
				}
				if (actions.containsKey(TOTALCOUNT)) {
					results.setTotal((Integer) actions.get(TOTALCOUNT));
				}
				// Jacoco
				if (actions.containsKey(JACOCO_BRANCH)) {
					Map<String, Object> tempMap = (Map<String, Object>) actions.get(JACOCO_BRANCH);
					results.setCcConditions((Integer) tempMap.get("percentage"));
				}
				if (actions.containsKey(JACOCO_CLASS)) {
					Map<String, Object> tempMap = (Map<String, Object>) actions.get(JACOCO_CLASS);
					results.setCcClasses((Integer) tempMap.get("percentage"));
				}
				if (actions.containsKey(JACOCO_LINES)) {
					Map<String, Object> tempMap = (Map<String, Object>) actions.get(JACOCO_LINES);
					results.setCcLines((Integer) tempMap.get("percentage"));
				}
			}
			if (actions.containsKey(JACOCO_METHODS)) {
				Map<String, Object> tempMap = (Map<String, Object>) actions.get(JACOCO_METHODS);
				results.setCcMethods((Integer) tempMap.get("percentage"));
			}
			if (actions.containsKey(SONAR_URL)) {
				results.setSonarUrl((String) actions.get(SONAR_URL));
			}
		}
		// Cobertura ?
		// Calculate Pass Results
		results.setPass(results.getTotal() - Math.abs(results.getFail()) - Math.abs(results.getSkip()));
		// Calculate Percentage
		results.setPercentageReport(Helper.singDoubleSingle((double) (results.getPass() + results.getSkip()) * 100 / results.getTotal()));
	}
}
