package com.jenkins.testresultsaggregator.helper;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jenkins.testresultsaggregator.data.Data;
import com.jenkins.testresultsaggregator.data.Job;
import com.jenkins.testresultsaggregator.data.JobStatus;
import com.jenkins.testresultsaggregator.data.JobWithDetailsAggregator;
import com.jenkins.testresultsaggregator.data.Results;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpConnection;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildChangeSet;
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
			if (index % 3 == 0) {
				Thread.sleep(2000);
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
				job.setJobDetails(getDetails(job));
				if (job.getJobDetails() == null) {
					job.setJobStatus(JobStatus.NOT_FOUND);
					job.setLastBuildResults(new Results(JobStatus.NOT_FOUND.name(), null));
					logger.println("Job '" + job.getJobName() + "' not found");
				} else if (job.getJobDetails().isBuildable() && job.getJobDetails().hasLastBuildRun()) {
					// Job FOUND
					job.setLastBuildDetails(getLastBuildDetails(job));
					job.setLastBuildNumber(job.getLastBuildDetails().getNumber());
					job.setJobStatus(JobStatus.FOUND);
					job.setLastBuildResults(new Results(JobStatus.FOUND.name(), null));
					job.setIsBuilding(job.getLastBuildDetails().isBuilding());
					job.setBuildNumber(job.getLastBuildDetails().getNumber());
					logger.print("Job '" + job.getJobName() + "' found #" + job.getLastBuildDetails().getNumber());
					if (job.getLastBuildDetails().isBuilding()) {
						logger.println(" : building");
						if (ignoreRunningJobs) {
							job.setJobStatus(JobStatus.RUNNING_REPORT_PREVIOUS);
							job.setLastBuildResults(new Results(JobStatus.RUNNING_REPORT_PREVIOUS.name(), null));
							// Resolve previous Saved data if any resolve it from builds
							Integer previousBuildNumber = null;
							if (job.getPreviousBuildNumber() == null) {
								previousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
								job.setLastBuildNumber(previousBuildNumber);
							} else {
								previousBuildNumber = job.getPreviousBuildNumber();
							}
							job.setLastBuildDetails(getBuildDetails(job, previousBuildNumber));
							job.setBuildNumber(previousBuildNumber);
							// Resolve the previous of the previous
							Integer previousOfPreviousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 3);
							job.setPreviousBuildNumber(previousOfPreviousBuildNumber);
							job.setPreviousBuildDetails(getBuildDetails(job, previousOfPreviousBuildNumber));
						} else {
							job.setJobStatus(JobStatus.RUNNING);
							job.setLastBuildResults(new Results(JobStatus.RUNNING.name(), null));
						}
					} else {
						logger.println(" : " + job.getLastBuildDetails().getResult().toString().toLowerCase());
						if (compareWithPreviousRun) {
							// Resolve previous Saved data if any resolve it from builds
							if (job.getPreviousBuildNumber() == null) {
								job.setPreviousBuildNumber(resolvePreviousBuildNumberFromBuild(job, 2));
							}
							if (job.getPreviousBuildNumber() < job.getLastBuildNumber()) {
								// Found new build get previous results
								job.setPreviousBuildDetails(getBuildDetails(job, job.getPreviousBuildNumber()));
								// job.setPreviousBuildResults(new Results());
							} else {
								// Job has already reported -> do not compare latest with previously saved by aggregator plugin, (equals)
							}
						}
					}
					
					job.setLastBuildResults(calculateResults(job, compareWithPreviousRun));
					logger.println(LocalMessages.COLLECT_DATA.toString() + " '" + job.getJobName() + "' " + LocalMessages.FINISHED.toString());
				} else {
					job.setJobStatus(JobStatus.DISABLED);
					job.setLastBuildResults(new Results(JobStatus.DISABLED.name(), null));
					logger.println("Job '" + job.getJobName() + "' found and it is not buildable or has no build run");
				}
			} catch (IOException e) {
				job.setJobStatus(JobStatus.NOT_FOUND);
				job.setLastBuildResults(new Results(JobStatus.NOT_FOUND.name(), null));
				logger.println("Job '" + job.getJobName() + "' not found with error : " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private Integer resolvePreviousBuildNumberFromBuild(Job job, int depth) {
		try {
			List<Integer> allBuildNumbers = job.getJobDetails().getAllBuilds().stream().map(Build::getNumber).collect(Collectors.toList());
			Collections.sort(allBuildNumbers);
			return allBuildNumbers.get(allBuildNumbers.size() - depth);
		} catch (Exception ex) {
			// ex.printStackTrace();
		}
		return null;
	}
	
	public Results calculateResults(Job job, boolean compareWithPreviousRun) {
		if (job != null && job.getLastBuildDetails() != null) {
			// Set Url
			job.getLastBuildResults().setUrl(job.getJobDetails().getUrl().toString());
			// Set Building status
			job.getLastBuildResults().setBuilding(job.getLastBuildDetails().isBuilding());
			// Set Current Result
			if (job.getLastBuildDetails().getResult() != null) {
				job.getLastBuildResults().setCurrentResult(job.getLastBuildDetails().getResult().name());
			}
			// Set Description
			job.getLastBuildResults().setDescription(job.getLastBuildDetails().getDescription());
			// Set Duration
			job.getLastBuildResults().setDuration(job.getLastBuildDetails().getDuration());
			// Set Number
			job.getLastBuildResults().setNumber(job.getLastBuildDetails().getNumber());
			// Set TimeStamp
			DateFormat formatter = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss:SSS");
			String dateFormatted = formatter.format(new Date(job.getLastBuildDetails().getTimestamp()));
			job.getLastBuildResults().setTimestamp(dateFormatted);
			if (!JobStatus.RUNNING.equals(job.getJobStatus())) {
				// Update for last build the actions
				actions(job.getLastBuildDetails(), job.getLastBuildResults());
				// Calculate Previous Results
				if (compareWithPreviousRun && job.getPreviousBuildDetails() != null) {
					if (job.getPreviousBuildResults() == null) {
						job.setPreviousBuildResults(new Results(job.getPreviousBuildDetails().getResult().name(), null));
					}
					// Update for previous build the actions
					actions(job.getPreviousBuildDetails(), job.getPreviousBuildResults());
					// Calculate diffs
					calculateDiffs(job);
				}
			}
			return job.getLastBuildResults();
		}
		return null;
	}
	
	private void calculateDiffs(Job job) {
		// Test diffs
		calculateTestResultsDiffs(job);
		// Coverage diffs
		calculateCodeCoverageDiffs(job);
		// Calculate Change Set
		calculateChangeSets(job);
	}
	
	private void calculateChangeSets(Job job) {
		if (job.getLastBuildDetails() != null) {
			if (job.getLastBuildDetails().getChangeSets() != null) {
				int changes = 0;
				for (BuildChangeSet tempI : job.getLastBuildDetails().getChangeSets()) {
					changes += tempI.getItems().size();
				}
				job.getLastBuildResults().setNumberOfChanges(changes);
			} else {
				job.getLastBuildResults().setNumberOfChanges(0);
			}
			// Set Changes URL
			job.getLastBuildResults().setChangesUrl(job.getLastBuildDetails().getUrl() + "/" + CHANGES);
			
		}
		// TODO More build and possible change sets between last and saved job, resolve them
	}
	
	private void calculateTestResultsDiffs(Job job) {
		job.getLastBuildResults().setTotalDif(job.getPreviousBuildResults().getTotal());
		job.getLastBuildResults().setPassDif(job.getPreviousBuildResults().getPass());
		job.getLastBuildResults().setFailDif(job.getPreviousBuildResults().getFail());
		job.getLastBuildResults().setSkipDif(job.getPreviousBuildResults().getSkip());
	}
	
	private void calculateCodeCoverageDiffs(Job job) {
		job.getLastBuildResults().setCcClasses(job.getPreviousBuildResults().getCcClassesDif());
		job.getLastBuildResults().setCcConditions(job.getPreviousBuildResults().getCcConditionsDif());
		job.getLastBuildResults().setCcFiles(job.getPreviousBuildResults().getCcFilesDif());
		job.getLastBuildResults().setCcLines(job.getPreviousBuildResults().getCcLinesDif());
		job.getLastBuildResults().setCcMethods(job.getPreviousBuildResults().getCcMethodsDif());
		job.getLastBuildResults().setCcPackages(job.getPreviousBuildResults().getCcPackagesDif());
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
		results.setPercentage(Helper.countPercentage(results));
		
	}
}
