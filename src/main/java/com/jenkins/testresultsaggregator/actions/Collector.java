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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
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
	public static final int parrallelThreads = 4;
	public static final int delayThreads = 4000;
	public static final int maxThreadTime = 120000;
	
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
			com.offbytwo.jenkins.model.Job modelJob = jobs.get(job.getJobNameOnly());
			if (modelJob != null) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = modelJob.getClient().get(modelJob.details().getUrl() + DEPTH, JobWithDetailsAggregator.class);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (NullPointerException e) {
						retries++;
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
					} catch (NullPointerException e) {
						retries++;
					}
					retries++;
				}
			}
		}
		return response;
	}
	
	public BuildWithDetailsAggregator getLastBuildDetails(Job job) throws Exception {
		BuildWithDetailsAggregator response = null;
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			com.offbytwo.jenkins.model.Job modelJob = jobs.get(job.getJobNameOnly());
			if (modelJob != null) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = modelJob.getClient().get(modelJob.details().getLastBuild().details().getUrl() + DEPTH, BuildWithDetailsAggregator.class);
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
						response = client.get(job.getUrl() + DEPTH, BuildWithDetailsAggregator.class);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (NullPointerException e) {
						retries++;
					}
					retries++;
				}
			}
		}
		return response;
	}
	
	public BuildWithDetailsAggregator getBuildDetails(Job job, Integer number) throws Exception {
		BuildWithDetailsAggregator response = null;
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			com.offbytwo.jenkins.model.Job modelJob = jobs.get(job.getJobNameOnly());
			if (modelJob != null && number != null && number > 0) {
				int retries = 1;
				while (retries < 4 && response == null) {
					try {
						response = modelJob.getClient().get(modelJob.details().getBuildByNumber(number).details().getUrl() + DEPTH, BuildWithDetailsAggregator.class);
					} catch (IOException ex) {
						ex.printStackTrace();
					} catch (NullPointerException ex) {
						// In case that the build number doesn't exists the exception is NPE
						// ex.printStackTrace();
						retries++;
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
							response = client.get(job.getUrl() + DEPTH, BuildWithDetailsAggregator.class);
						} catch (IOException ex) {
							ex.printStackTrace();
						} catch (NullPointerException ex) {
							// In case that the build number doesn't exists the exception is NPE
							// ex.printStackTrace();
							retries++;
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
			if (index % parrallelThreads == 0) {
				Thread.sleep(delayThreads);
			}
		}
		for (ReportThread thread : threads) {
			thread.join(maxThreadTime);
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
			Stopwatch stopwatch = Stopwatch.createStarted();
			StringBuilder text = new StringBuilder();
			try {
				job.setJob(getDetails(job));
				if (job.getJob() == null) {
					text.append("Job '" + job.getJobName() + "' found " + JobStatus.NOT_FOUND.name());
					job.setResults(new Results(JobStatus.NOT_FOUND.name(), job.getUrl()));
				} else if (!job.getJob().isBuildable()) {
					text.append("Job '" + job.getJobName() + "' found " + JobStatus.DISABLED.name());
					job.setResults(new Results(JobStatus.DISABLED.name(), job.getUrl()));
				} else if (job.getJob().isBuildable() && !job.getJob().hasLastBuildRun()) {
					text.append("Job '" + job.getJobName() + "' found " + JobStatus.NO_LAST_BUILD_DATA.name());
					job.setResults(new Results(JobStatus.NO_LAST_BUILD_DATA.name(), job.getUrl()));
				} else {
					// Job FOUND
					job.setLast(new BuildWithDetailsAggregator());
					job.setLast(getLastBuildDetails(job));
					job.getLast().setResults(calculateResults(job.getLast()));
					job.setIsBuilding(job.getLast().isBuilding());
					text.append("Job '" + job.getJobName() + "' found build number " + job.getLast().getNumber() + " with status");
					if (job.getLast().isBuilding()) {
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
								text.append(" building(Results), previous build " + previousBuildNumber);
							} else {
								text.append(" building, previous build " + previousBuildNumber);
							}
							job.setLast(new BuildWithDetailsAggregator());
							job.setLast(getBuildDetails(job, previousBuildNumber));
							job.getLast().setBuildNumber(previousBuildNumber);
							job.getLast().setResults(calculateResults(job.getLast()));
							//
							job.setPrevious(new BuildWithDetailsAggregator());
							job.setPrevious(job.getLast());
							job.getPrevious().setBuildNumber(previousBuildNumber);
							job.getPrevious().setResults(job.getLast().getResults());
						} else {
							text.append(" running");
							job.setResults(new Results(JobStatus.RUNNING.name(), job.getUrl()));
						}
					} else {
						text.append(" " + job.getLast().getResult().toString().toLowerCase());
						if (compareWithPreviousRun) {
							Integer previousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
							Integer previousBuildNumberSaved = 0;
							if (job.getResults() == null) {
								// Not Found previously saved , resolve from jenkins
								job.setResults(new Results(JobStatus.FOUND.name(), job.getUrl()));
							} else {
								// Found previously saved use them
								previousBuildNumberSaved = job.getResults().getNumber();
							}
							if (previousBuildNumberSaved > 0) {
								previousBuildNumber = previousBuildNumberSaved;
							}
							if (previousBuildNumber == job.getLast().getNumber()) {
								// There is no new run since the previous aggregator run
								job.setLast(getBuildDetails(job, previousBuildNumber));
								job.getLast().setBuildNumber(previousBuildNumber);
								job.getLast().setResults(calculateResults(job.getLast()));
								//
								Integer previousOfPreviousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
								job.setPrevious(getBuildDetails(job, previousOfPreviousBuildNumber));
								job.getPrevious().setBuildNumber(previousOfPreviousBuildNumber);
								job.getPrevious().setResults(calculateResults(job.getPrevious()));
								// Results
								job.setResults(new Results(JobStatus.FOUND.name(), job.getUrl()));
							} else {
								job.setPrevious(getBuildDetails(job, previousBuildNumber));
								job.getPrevious().setBuildNumber(previousBuildNumber);
								job.getPrevious().setResults(calculateResults(job.getPrevious()));
								// Results
								job.setResults(new Results(JobStatus.FOUND.name(), job.getUrl()));
							}
						} else {
							job.setPrevious(job.getLast());
							job.getPrevious().setBuildNumber(job.getLast().getNumber());
							job.getPrevious().setResults(calculateResults(job.getPrevious()));
						}
					}
					text.append(LocalMessages.FINISHED.toString());
				}
			} catch (Exception e) {
				text.append("Job '" + job.getJobName() + "' found " + JobStatus.NOT_FOUND.name() + "with error : " + e.getMessage());
				job.setResults(new Results(JobStatus.NOT_FOUND.name(), job.getUrl()));
				e.printStackTrace();
			}
			stopwatch.stop();
			text.append(" (" + stopwatch.elapsed(TimeUnit.SECONDS) + "s)");
			logger.println(text.toString());
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
