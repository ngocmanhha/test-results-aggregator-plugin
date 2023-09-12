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
	
	public JobWithDetailsAggregator getDetails(Job job) throws Exception {
		JobWithDetailsAggregator response = null;
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			if (jobs.get(job.getJobNameOnly()) != null) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = jobs.get(job.getJobNameOnly()).getClient().get(jobs.get(job.getJobNameOnly()).details().getUrl() + DEPTH, JobWithDetailsAggregator.class);
					} catch (IOException e) {
						e.printStackTrace();
					}
					retries++;
				}
			}
		} else {
			JenkinsHttpConnection client = null;
			try {
				client = jenkins.getQueue().getClient();
			} catch (IOException ex) {
				throw new Exception(ex);
			}
			if (client != null) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = client.get(job.getUrl() + DEPTH, JobWithDetailsAggregator.class);
					} catch (IOException e) {
						e.printStackTrace();
					}
					retries++;
				}
			}
		}
		return response;
	}
	
	public BuildWithDetails getLastBuildDetails(Job job) throws Exception {
		BuildWithDetails response = null;
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			if (jobs.get(job.getJobNameOnly()) != null) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = jobs.get(job.getJobNameOnly()).getClient().get(jobs.get(job.getJobNameOnly()).details().getLastBuild().details().getUrl() + DEPTH, BuildWithDetails.class);
					} catch (Exception e) {
						e.printStackTrace();
					}
					retries++;
				}
			}
		} else {
			JenkinsHttpConnection client = null;
			try {
				client = jenkins.getQueue().getClient();
			} catch (IOException ex) {
				throw new Exception(ex);
			}
			if (client != null) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = client.get(job.getUrl() + DEPTH, BuildWithDetails.class);
					} catch (IOException e) {
						e.printStackTrace();
					}
					retries++;
				}
			}
		}
		return response;
	}
	
	public BuildWithDetails getBuildDetails(Job job, Integer number) throws Exception {
		BuildWithDetails response = null;
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			if (jobs.get(job.getJobNameOnly()) != null && number != null && number > 0) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = jobs.get(job.getJobNameOnly()).getClient().get(jobs.get(job.getJobNameOnly()).details().getBuildByNumber(number).details().getUrl() + DEPTH, BuildWithDetails.class);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					retries++;
				}
			} else {
				JenkinsHttpConnection client = null;
				try {
					client = jenkins.getQueue().getClient();
				} catch (IOException ex) {
					throw new Exception(ex);
				}
				if (client != null) {
					int retries = 1;
					while (retries < 4 && response == null) {
						try {
							response = client.get(job.getUrl() + DEPTH, BuildWithDetails.class);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
						retries++;
					}
				}
			}
		}
		return response;
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
			thread.join(120000);
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
					job.getLast().setResults(calculateResults(job.getLast().getBuildDetails()));
					job.setIsBuilding(job.getLast().getBuildDetails().isBuilding());
					logger.print("Job '" + job.getJobName() + "' found build number " + job.getLast().getBuildDetails().getNumber() + " with status");
					if (job.getLast().getBuildDetails().isBuilding()) {
						if (ignoreRunningJobs && compareWithPreviousRun) {
							int previousBuildNumber = 0;
							int previousBuildNumber2 = 0;
							if (job.getResults() == null) {
								job.setResults(new Results(JobStatus.RUNNING_REPORT_PREVIOUS.name(), job.getUrl()));
							} else {
								job.getResults().setStatus(JobStatus.RUNNING_REPORT_PREVIOUS.name());
								job.getResults().setUrl(job.getUrl());
								previousBuildNumber2 = job.getResults().getNumber();
							}
							previousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
							if (previousBuildNumber2 < previousBuildNumber) {
								previousBuildNumber = previousBuildNumber2;
								logger.println(" building2, previous build " + previousBuildNumber);
							} else {
								logger.println(" building, previous build " + previousBuildNumber);
							}
							job.setLast(new BuildWithDetailsAggregator());
							job.getLast().setBuildNumber(previousBuildNumber);
							job.getLast().setBuildDetails(getBuildDetails(job, previousBuildNumber));
							job.getLast().setResults(calculateResults(job.getLast().getBuildDetails()));
							//
							job.setPrevious(new BuildWithDetailsAggregator());
							job.getPrevious().setBuildNumber(previousBuildNumber);
							job.getPrevious().setBuildDetails(job.getLast().getBuildDetails());
							job.getPrevious().setResults(job.getLast().getResults());
						} else {
							logger.println(" running");
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
								job.getLast().setResults(calculateResults(job.getLast().getBuildDetails()));
								//
								Integer previousOfPreviousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
								job.setPrevious(new BuildWithDetailsAggregator());
								job.getPrevious().setBuildNumber(previousOfPreviousBuildNumber);
								job.getPrevious().setBuildDetails(getBuildDetails(job, previousOfPreviousBuildNumber));
								job.getPrevious().setResults(calculateResults(job.getPrevious().getBuildDetails()));
							} else {
								job.setPrevious(new BuildWithDetailsAggregator());
								job.getPrevious().setBuildNumber(previousBuildNumber);
								job.getPrevious().setBuildDetails(getBuildDetails(job, previousBuildNumber));
								job.getPrevious().setResults(calculateResults(job.getPrevious().getBuildDetails()));
							}
						} else {
							job.setPrevious(new BuildWithDetailsAggregator());
							job.getPrevious().setBuildNumber(job.getLast().getBuildDetails().getNumber());
							job.getPrevious().setBuildDetails(job.getLast().getBuildDetails());
							job.getPrevious().setResults(calculateResults(job.getPrevious().getBuildDetails()));
						}
					}
					logger.println(LocalMessages.COLLECT_DATA.toString() + " '" + job.getJobName() + "' " + LocalMessages.FINISHED.toString());
				}
			} catch (Exception e) {
				logger.println("Job '" + job.getJobName() + "' found " + JobStatus.NOT_FOUND.name() + "with error : " + e.getMessage());
				job.setResults(new Results(JobStatus.NOT_FOUND.name(), job.getUrl()));
				e.printStackTrace();
			}
		}
	}
	
	////
	private JobResults calculateResults(BuildWithDetails buildWithDetails) {
		JobResults jobResults = new JobResults();
		if (buildWithDetails != null) {
			return new CollectorHelper(jobResults, buildWithDetails).calculate();
		}
		return jobResults;
	}
	
	private int resolvePreviousBuildNumberFromBuild(Job job, int depth) {
		try {
			// TODO : Retries here
			List<Integer> allBuildNumbers = job.getJob().getAllBuilds().stream().map(Build::getNumber).collect(Collectors.toList());
			int retries = 1;
			while ((allBuildNumbers == null || allBuildNumbers.isEmpty()) && retries < 4) {
				allBuildNumbers = job.getJob().getAllBuilds().stream().map(Build::getNumber).collect(Collectors.toList());
				retries++;
			}
			if (allBuildNumbers != null) {
				Collections.sort(allBuildNumbers);
				Integer found = allBuildNumbers.get(allBuildNumbers.size() - depth);
				if (found == null) {
					return 0;
				}
				return found.intValue();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}
	
}
