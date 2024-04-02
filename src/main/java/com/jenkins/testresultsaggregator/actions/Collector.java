package com.jenkins.testresultsaggregator.actions;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.jenkins.testresultsaggregator.data.BuildWithDetailsAggregator;
import com.jenkins.testresultsaggregator.data.Data;
import com.jenkins.testresultsaggregator.data.Job;
import com.jenkins.testresultsaggregator.data.JobResults;
import com.jenkins.testresultsaggregator.data.JobStatus;
import com.jenkins.testresultsaggregator.data.JobWithDetailsAggregator;
import com.jenkins.testresultsaggregator.data.Results;
import com.jenkins.testresultsaggregator.helper.LocalMessages;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpConnection;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;

import hudson.util.Secret;

public class Collector {
	
	private int mode = 2;
	public static final String ROOT_FOLDER = "root";
	public static final int parrallelThreads = 6;
	public static final int delayThreads = 6000;
	public static final int maxThreadTime = 120000;
	// Urls
	public static final String DEPTH = "?depth=1";
	
	private PrintStream logger;
	JenkinsServer jenkins;
	Map<String, com.offbytwo.jenkins.model.Job> jobs;
	JenkinsHttpConnection client;
	
	public Collector(String jenkinsUrl, String username, Secret password, PrintStream printStream, List<Data> data) throws URISyntaxException, IOException {
		this.logger = printStream;
		this.jenkins = new JenkinsServer(new URI(jenkinsUrl), username, password.getPlainText());
		this.jobs = jenkins.getJobs();
		this.client = jenkins.getQueue().getClient();
		StringBuilder text = new StringBuilder();
		text.append("Total Jenkins jobs found " + jobs.size());
		List<Job> list = new ArrayList<>();
		for (Data temp : data) {
			list.addAll(temp.getJobs());
		}
		int successResolvedModel = 0;
		for (Job temp : list) {
			try {
				resolveModel(temp, null);
				if (temp.getModelJob() != null) {
					successResolvedModel++;
				}
			} catch (Exception ex) {
				logger.println("Error resolve model for job " + temp.getJobName() + " " + ex.getMessage());
			}
		}
		text.append(" and Aggregator has resolved successfully " + successResolvedModel + "/" + list.size());
		logger.println(text.toString());
	}
	
	public void closeJenkinsConnection() {
		if (jenkins != null) {
			jenkins.close();
		}
	}
	
	private Job resolveModel(Job job, FolderJob folderJob) throws IOException {
		if (job.getFolder().equalsIgnoreCase(ROOT_FOLDER)) {
			job.setModelJob(jobs.get(job.getJobNameOnly()));
		} else {
			if (folderJob == null) {
				for (String tempFolder : job.getFolders()) {
					com.offbytwo.jenkins.model.Job found = jobs.get(tempFolder);
					if (found != null) {
						resolveModel(job, client.get(found.getUrl(), FolderJob.class));
						break;
					}
				}
			} else {
				Map<String, com.offbytwo.jenkins.model.Job> jobsIntoFolder = jenkins.getJobs(folderJob);
				if (jobsIntoFolder != null && jobsIntoFolder.containsKey(job.getJobNameOnly())) {
					job.setModelJob(jobsIntoFolder.get(job.getJobNameOnly()));
				} else {
					for (String tempFolder : job.getFolders()) {
						if (jobsIntoFolder.get(tempFolder) != null) {
							FolderJob found = client.get(jobsIntoFolder.get(tempFolder).getUrl(), FolderJob.class);
							if (found != null) {
								resolveModel(job, found);
							}
						}
					}
				}
			}
		}
		return job;
	}
	
	public JobWithDetailsAggregator getDetails(Job job) throws Exception {
		return getDetailsMode(job, mode);
	}
	
	public BuildWithDetailsAggregator getLastBuildDetails(Job job) throws Exception {
		return getLastBuildDetailsMode(job, mode);
	}
	
	public BuildWithDetailsAggregator getBuildDetails(Job job, Integer number) throws Exception {
		return getBuildDetailsMode(job, number, mode);
	}
	
	private JobWithDetailsAggregator getDetailsMode(Job job, int mode) throws Exception {
		JobWithDetailsAggregator response = null;
		int retries = 0;
		StringBuilder errorFound = new StringBuilder();
		while (retries < 4 && response == null) {
			try {
				if (mode == 1) {
					response = job.getModelJob().getClient().get(job.getModelJob().details().getUrl() + DEPTH, JobWithDetailsAggregator.class);
				} else {
					response = client.get(job.getUrl(), JobWithDetailsAggregator.class);
				}
				errorFound = new StringBuilder();
			} catch (Exception ex) {
				if (ex.getMessage() != null && ex.getMessage().endsWith("is null")) {
					throw ex;
				} else {
					errorFound = new StringBuilder();
					errorFound.append("Error get details for job " + job.getJobName() + " " + ex.getMessage());
				}
			}
			retries++;
			mode = (retries % 2) + 1;
		}
		if (!Strings.isNullOrEmpty(errorFound.toString())) {
			logger.println("ERROR " + errorFound.toString());
		}
		return response;
	}
	
	private BuildWithDetailsAggregator getLastBuildDetailsMode(Job job, int mode) throws Exception {
		BuildWithDetailsAggregator response = null;
		int retries = 0;
		StringBuilder errorFound = new StringBuilder();
		while (retries < 4 && response == null) {
			try {
				if (mode == 1) {
					response = job.getModelJob().getClient().get(job.getModelJob().details().getLastBuild().details().getUrl() + DEPTH, BuildWithDetailsAggregator.class);
				} else {
					response = client.get(job.getModelJob().details().getLastBuild().details().getUrl() + DEPTH, BuildWithDetailsAggregator.class);
				}
				errorFound = new StringBuilder();
			} catch (Exception ex) {
				if (ex.getMessage() != null && ex.getMessage().endsWith("is null")) {
					throw ex;
				} else {
					errorFound = new StringBuilder();
					errorFound.append("No last build details for job " + job.getJobName() + " " + ex.getMessage());
				}
			}
			retries++;
			mode = (retries % 2) + 1;
		}
		if (!Strings.isNullOrEmpty(errorFound.toString())) {
			logger.println("ERROR " + errorFound.toString());
		}
		return response;
	}
	
	public BuildWithDetailsAggregator getBuildDetailsMode(Job job, Integer number, int mode) throws Exception {
		BuildWithDetailsAggregator response = null;
		StringBuilder errorFound = new StringBuilder();
		if (number != null && number > 0) {
			int retries = 0;
			Build build = null;
			while (retries < 4 && response == null) {
				try {
					if (build == null) {
						build = job.getModelJob().details().getBuildByNumber(number);
					}
					if (build != null) {
						if (mode == 1) {
							response = job.getModelJob().getClient().get(build.details().getUrl() + DEPTH, BuildWithDetailsAggregator.class);
						} else {
							response = client.get(build.details().getUrl() + DEPTH, BuildWithDetailsAggregator.class);
						}
					}
					errorFound = new StringBuilder();
				} catch (Exception ex) {
					if (ex.getMessage() != null && ex.getMessage().endsWith("is null")) {
						throw ex;
					} else {
						errorFound = new StringBuilder();
						errorFound.append("No build details for job " + job.getJobName() + " with number " + number + " " + ex.getMessage());
					}
				}
				retries++;
				mode = (retries % 2) + 1;
			}
		}
		if (!Strings.isNullOrEmpty(errorFound.toString())) {
			logger.println("ERROR " + errorFound.toString());
		}
		return response;
	}
	
	public void collectResults(List<Data> dataJob, boolean compareWithPreviousRun, Boolean ignoreRunningJobs) throws InterruptedException {
		logger.println("Collect data");
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
		logger.println("Collect data ...Finished");
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
			if (job.getModelJob() != null) {
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
							if (ignoreRunningJobs) {
								int previousBuildNumberFromJenkins = 0;
								int previousBuildNumberFromResults = 0;
								if (job.getResults() == null) {
									job.setResults(new Results(JobStatus.RUNNING_REPORT_PREVIOUS.name(), job.getUrl()));
								} else {
									job.getResults().setStatus(JobStatus.RUNNING_REPORT_PREVIOUS.name());
									job.getResults().setUrl(job.getUrl());
									previousBuildNumberFromResults = job.getResults().getNumber();
								}
								boolean foundResults = false;
								if (previousBuildNumberFromResults > 0) {
									// Found previousBuildNumberFromResults
									BuildWithDetailsAggregator previousResult = getBuildDetails(job, previousBuildNumberFromResults);
									if (previousResult != null) {
										text.append(" building(Results), previous build " + previousBuildNumberFromResults);
										job.setLast(previousResult);
										job.getLast().setBuildNumber(previousBuildNumberFromResults);
										job.getLast().setResults(calculateResults(job.getLast()));
										//
										job.setPrevious(new BuildWithDetailsAggregator());
										job.setPrevious(job.getLast());
										job.getPrevious().setBuildNumber(previousBuildNumberFromResults);
										job.getPrevious().setResults(job.getLast().getResults());
										foundResults = true;
									}
								}
								if (!foundResults) {
									previousBuildNumberFromJenkins = resolvePreviousBuildNumberFromBuild(job, 2);
									if (previousBuildNumberFromJenkins > 0) {
										BuildWithDetailsAggregator previousResult = getBuildDetails(job, previousBuildNumberFromJenkins);
										if (previousResult != null && previousBuildNumberFromJenkins > 0) {
											text.append(" building, previous build " + previousBuildNumberFromJenkins);
											job.setLast(previousResult);
											job.getLast().setBuildNumber(previousBuildNumberFromJenkins);
											job.getLast().setResults(calculateResults(job.getLast()));
											//
											job.setPrevious(new BuildWithDetailsAggregator());
											job.setPrevious(job.getLast());
											job.getPrevious().setBuildNumber(previousBuildNumberFromJenkins);
											job.getPrevious().setResults(job.getLast().getResults());
										}
									} else {
										job.setResults(new Results(JobStatus.RUNNING.name(), job.getUrl()));
									}
								}
							} else {
								text.append(" running");
								job.setResults(new Results(JobStatus.RUNNING.name(), job.getUrl()));
							}
						} else {
							if (job.getLast().getResult() != null) {
								text.append(" " + job.getLast().getResult().toString().toLowerCase());
							}
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
									BuildWithDetailsAggregator previousResult = getBuildDetails(job, previousBuildNumber);
									if (previousResult != null && previousBuildNumber > 0) {
										job.setLast(previousResult);
										job.getLast().setBuildNumber(previousBuildNumber);
										job.getLast().setResults(calculateResults(job.getLast()));
									}
									//
									Integer previousOfPreviousBuildNumber = resolvePreviousBuildNumberFromBuild(job, 2);
									if (previousOfPreviousBuildNumber > 0) {
										BuildWithDetailsAggregator previousOfPreviousResult = getBuildDetails(job, previousOfPreviousBuildNumber);
										if (previousOfPreviousResult != null && previousOfPreviousBuildNumber > 0) {
											job.setPrevious(previousOfPreviousResult);
											job.getPrevious().setBuildNumber(previousOfPreviousBuildNumber);
											job.getPrevious().setResults(calculateResults(job.getPrevious()));
										}
									}
									// Results
									job.setResults(new Results(JobStatus.FOUND.name(), job.getUrl()));
								} else {
									// Resolve previous from Jenkins
									BuildWithDetailsAggregator previousResult = getBuildDetails(job, previousBuildNumber);
									if (previousResult != null && previousBuildNumber > 0) {
										job.setPrevious(previousResult);
										job.getPrevious().setBuildNumber(previousBuildNumber);
										job.getPrevious().setResults(calculateResults(job.getPrevious()));
									}
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
					text.append("Job '" + job.getJobName() + "' found " + JobStatus.NOT_FOUND.name() + " with error : " + e.getMessage());
					job.setResults(new Results(JobStatus.NOT_FOUND.name(), job.getUrl()));
				}
			} else {
				text.append("Job '" + job.getJobName() + "' " + JobStatus.NOT_FOUND.name() + " url " + job.getUrl());
				job.setResults(new Results(JobStatus.NOT_FOUND.name(), job.getUrl()));
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
			List<Integer> allBuildNumbers = job.getJob().getAllBuilds().stream().map(Build::getNumber).collect(Collectors.toList());
			int retries = 1;
			while ((allBuildNumbers == null || allBuildNumbers.isEmpty()) && retries < 4) {
				allBuildNumbers = job.getJob().getAllBuilds().stream().map(Build::getNumber).collect(Collectors.toList());
				retries++;
			}
			if (allBuildNumbers != null && !allBuildNumbers.isEmpty()) {
				Collections.sort(allBuildNumbers);
				Integer found = allBuildNumbers.get(allBuildNumbers.size() - depth);
				if (found == null) {
					return 0;
				}
				return found.intValue();
			}
		} catch (Exception ex) {
			// ex.printStackTrace();
		}
		return 0;
	}
	
}
