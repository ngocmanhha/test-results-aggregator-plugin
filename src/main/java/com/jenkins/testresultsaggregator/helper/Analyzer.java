package com.jenkins.testresultsaggregator.helper;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Strings;
import com.jenkins.testresultsaggregator.TestResultsAggregator;
import com.jenkins.testresultsaggregator.TestResultsAggregator.AggregatorProperties;
import com.jenkins.testresultsaggregator.data.Aggregated;
import com.jenkins.testresultsaggregator.data.Data;
import com.jenkins.testresultsaggregator.data.Job;
import com.jenkins.testresultsaggregator.data.JobStatus;
import com.jenkins.testresultsaggregator.data.ReportGroup;
import com.jenkins.testresultsaggregator.data.ReportJob;
import com.jenkins.testresultsaggregator.data.Results;

public class Analyzer {
	
	private PrintStream logger;
	
	public Analyzer(PrintStream logger) {
		this.logger = logger;
	}
	
	public Aggregated analyze(List<Data> listData, Properties properties) throws Exception {
		// Resolve
		String outOfDateResults = properties.getProperty(TestResultsAggregator.AggregatorProperties.OUT_OF_DATE_RESULTS_ARG.name());
		// Check if Groups/Names are used
		boolean foundAtLeastOneGroupName = false;
		for (Data data : listData) {
			if (!Strings.isNullOrEmpty(data.getGroupName())) {
				foundAtLeastOneGroupName = true;
			}
		}
		// Order List per Group Name
		if (foundAtLeastOneGroupName) {
			Collections.sort(listData, new Comparator<Data>() {
				@Override
				public int compare(Data dataDTO1, Data dataDTO2) {
					return dataDTO1.getGroupName().compareTo(dataDTO2.getGroupName());
				}
			});
		}
		Aggregated aggregated = new Aggregated();
		// Calculate Aggregated Results for Reporting
		Results totalResults = new Results();
		for (Data data : listData) {
			boolean foundFailure = false;
			boolean foundRunning = false;
			boolean foundSkip = false;
			boolean foundDisabled = false;
			Results resultsPerGroup = new Results();
			int jobFailed = 0;
			int jobUnstable = 0;
			int jobAborted = 0;
			int jobSuccess = 0;
			int jobRunning = 0;
			int jobDisabled = 0;
			boolean isOnlyTestIntoGroup = true;
			data.setReportGroup(new ReportGroup());
			
			for (Job job : data.getJobs()) {
				job.setReport(new ReportJob());
				// Calculate Job Status
				job.getReport().calculateStatus(job);
				if (job.getLastBuildResults() != null && !job.getLastBuildResults().getStatus().equals(JobStatus.NOT_FOUND.name())) {
					// Report URL
					job.getReport().setReportURL(job.getLastBuildResults().getUrl());
					// Calculate Total
					job.getReport().calculateTotal(job.getLastBuildResults());
					// Calculate Pass
					job.getReport().calculatePass(job.getLastBuildResults());
					// Calculate Fail
					job.getReport().calculateFailedColor(job.getLastBuildResults());
					// Calculate Skipped
					job.getReport().calculateSkipped(job.getLastBuildResults());
					// Calculate timestamp
					job.getReport().calculateTimestamp(job.getLastBuildResults(), outOfDateResults);
					// Calculate Changes
					job.getReport().calculateChanges(job.getLastBuildResults());
					// Calculate Sonar Url
					job.getReport().calculateSonar(job.getLastBuildResults());
					// Calculate Coverage Packages
					job.getReport().calculateCCPackages(job.getLastBuildResults());
					job.getReport().calculateCCFiles(job.getLastBuildResults());
					job.getReport().calculateCCClasses(job.getLastBuildResults());
					job.getReport().calculateCCMethods(job.getLastBuildResults());
					job.getReport().calculateCCLines(job.getLastBuildResults());
					job.getReport().calculateCCConditions(job.getLastBuildResults());
					
					// Calculate Duration
					if (job.getLastBuildDetails() != null) {
						job.getReport().calculateDuration(job.getLastBuildDetails().getDuration());
						// Total Duration
						aggregated.setTotalDuration(aggregated.getTotalDuration() + job.getLastBuildDetails().getDuration());
						// Total Changes
						aggregated.setTotalNumberOfChanges(aggregated.getTotalNumberOfChanges() + job.getLastBuildResults().getNumberOfChanges());
						// Calculate Description
						job.getReport().calculateDescription(job.getLastBuildDetails().getDescription());
					}
					// Calculate Percentage
					job.getReport().calculatePercentage(job.getLastBuildResults());
					// Calculate Group
					String jobStatus = job.getReport().getStatus();
					if (jobStatus != null) {
						if (jobStatus.startsWith(JobStatus.SUCCESS.name())) {
							data.getReportGroup().setJobSuccess(data.getReportGroup().getJobSuccess() + 1);
							aggregated.setSuccessJobs(aggregated.getSuccessJobs() + 1);
							jobSuccess++;
						} else if (jobStatus.startsWith(JobStatus.FIXED.name())) {
							data.getReportGroup().setJobSuccess(data.getReportGroup().getJobSuccess() + 1);
							aggregated.setFixedJobs(aggregated.getFixedJobs() + 1);
							jobSuccess++;
						} else if (jobStatus.startsWith(JobStatus.RUNNING.name()) && "false".equalsIgnoreCase((String) properties.get(AggregatorProperties.IGNORE_RUNNING_JOBS.name()))) {
							foundRunning = true;
							data.getReportGroup().setJobRunning(data.getReportGroup().getJobRunning() + 1);
							aggregated.setRunningJobs(aggregated.getRunningJobs() + 1);
							jobRunning++;
						} else if (jobStatus.startsWith(JobStatus.FAILURE.name())) {
							foundFailure = true;
							data.getReportGroup().setJobFailed(data.getReportGroup().getJobFailed() + 1);
							aggregated.setFailedJobs(aggregated.getFailedJobs() + 1);
							jobFailed++;
						} else if (jobStatus.startsWith(JobStatus.STILL_FAILING.name())) {
							foundFailure = true;
							data.getReportGroup().setJobFailed(data.getReportGroup().getJobFailed() + 1);
							aggregated.setKeepFailJobs(aggregated.getKeepFailJobs() + 1);
							jobFailed++;
						} else if (jobStatus.startsWith(JobStatus.UNSTABLE.name())) {
							foundSkip = true;
							data.getReportGroup().setJobUnstable(data.getReportGroup().getJobUnstable() + 1);
							aggregated.setUnstableJobs(aggregated.getUnstableJobs() + 1);
							jobUnstable++;
						} else if (jobStatus.startsWith(JobStatus.STILL_UNSTABLE.name())) {
							foundSkip = true;
							data.getReportGroup().setJobUnstable(data.getReportGroup().getJobUnstable() + 1);
							aggregated.setKeepUnstableJobs(aggregated.getKeepUnstableJobs() + 1);
							jobUnstable++;
						} else if (jobStatus.startsWith(JobStatus.ABORTED.name()) && "false".equalsIgnoreCase((String) properties.get(AggregatorProperties.IGNORE_ABORTED_JOBS.name()))) {
							foundSkip = true;
							data.getReportGroup().setJobAborted(data.getReportGroup().getJobAborted() + 1);
							aggregated.setAbortedJobs(aggregated.getAbortedJobs() + 1);
							jobAborted++;
						} else if (jobStatus.startsWith(JobStatus.DISABLED.name()) && "false".equalsIgnoreCase((String) properties.get(AggregatorProperties.IGNORE_DISABLED_JOBS.name()))) {
							foundDisabled = true;
							data.getReportGroup().setJobDisabled(data.getReportGroup().getJobDisabled() + 1);
							aggregated.setDisabledJobs(aggregated.getDisabledJobs() + 1);
							jobDisabled++;
						}
					}
					// Calculate Total Tests Per Group
					resultsPerGroup.setPass(resultsPerGroup.getPass() + job.getLastBuildResults().getPass());
					resultsPerGroup.setSkip(resultsPerGroup.getSkip() + job.getLastBuildResults().getSkip());
					resultsPerGroup.setFail(resultsPerGroup.getFail() + job.getLastBuildResults().getFail());
					resultsPerGroup.setTotal(resultsPerGroup.getTotal() + job.getLastBuildResults().getTotal());
					// Calculate Total Tests for Summary Column
					totalResults.addResults(job.getLastBuildResults());
					// Has tests
					if (job.getLastBuildResults().getTotal() <= 0) {
						isOnlyTestIntoGroup = false;
					}
				}
			}
			// Set Results Per Group
			data.getReportGroup().setResults(resultsPerGroup);
			// Calculate Group Status
			if (foundRunning) {
				data.getReportGroup().setStatus(JobStatus.RUNNING.name());
			} else if (foundFailure) {
				data.getReportGroup().setStatus(JobStatus.FAILURE.name());
			} else if (foundSkip) {
				data.getReportGroup().setStatus(JobStatus.UNSTABLE.name());
			} else {
				data.getReportGroup().setStatus(JobStatus.SUCCESS.name());
			}
			// Set status if only tests
			data.getReportGroup().setOnlyTests(isOnlyTestIntoGroup);
			// Calculate Percentage Per Group based on Jobs
			if (!isOnlyTestIntoGroup) {
				data.getReportGroup().setPercentageForJobs(Helper.countPercentage(jobSuccess + jobUnstable, jobSuccess + jobRunning + jobAborted + jobUnstable + jobFailed));
			}
			// Calculate Percentage Per Group based on Tests
			// Skip tests are calculated as success into test percentage
			data.getReportGroup().setPercentageForTests(Helper.countPercentageD(resultsPerGroup.getPass() + resultsPerGroup.getSkip(),
					resultsPerGroup.getPass() + resultsPerGroup.getFail() + resultsPerGroup.getSkip()).toString());
		}
		// Order Jobs per
		final String orderBy = (String) properties.get(TestResultsAggregator.AggregatorProperties.SORT_JOBS_BY.name());
		for (Data data : listData) {
			Collections.sort(data.getJobs(), new Comparator<Job>() {
				@Override
				public int compare(Job dataJobDTO1, Job dataJobDTO2) {
					try {
						if (TestResultsAggregator.SortResultsBy.NAME.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getJobNameFromFriendlyName().compareTo(dataJobDTO2.getJobNameFromFriendlyName());
						} else if (TestResultsAggregator.SortResultsBy.STATUS.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getReport().getStatusFromEnum().getPriority() - dataJobDTO2.getReport().getStatusFromEnum().getPriority();
						} else if (TestResultsAggregator.SortResultsBy.TOTAL_TEST.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getLastBuildResults().getTotal() - dataJobDTO1.getLastBuildResults().getTotal();
						} else if (TestResultsAggregator.SortResultsBy.PASS.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getLastBuildResults().getPass() - dataJobDTO1.getLastBuildResults().getPass();
						} else if (TestResultsAggregator.SortResultsBy.FAIL.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getLastBuildResults().getFail() - dataJobDTO1.getLastBuildResults().getFail();
						} else if (TestResultsAggregator.SortResultsBy.SKIP.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getLastBuildResults().getSkip() - dataJobDTO1.getLastBuildResults().getSkip();
						} else if (TestResultsAggregator.SortResultsBy.LAST_RUN.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getLastBuildResults().getTimestamp().compareTo(dataJobDTO2.getLastBuildResults().getTimestamp());
						} else if (TestResultsAggregator.SortResultsBy.COMMITS.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getLastBuildResults().getNumberOfChanges() - dataJobDTO2.getLastBuildResults().getNumberOfChanges();
						} else if (TestResultsAggregator.SortResultsBy.DURATION.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getLastBuildResults().getDuration().compareTo(dataJobDTO2.getLastBuildResults().getDuration());
						} else if (TestResultsAggregator.SortResultsBy.PERCENTAGE.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getLastBuildResults().getPercentage().compareTo(dataJobDTO2.getLastBuildResults().getPercentage());
						} else if (TestResultsAggregator.SortResultsBy.BUILD_NUMBER.name().equalsIgnoreCase(orderBy)) {
							return Integer.toString(dataJobDTO1.getJobDetails().getNextBuildNumber()).compareTo(Integer.toString(dataJobDTO2.getJobDetails().getNextBuildNumber()));
						} else {
							// Default
							return dataJobDTO1.getJobNameFromFriendlyName().compareTo(dataJobDTO2.getJobNameFromFriendlyName());
						}
					} catch (NullPointerException ex) {
						return -1;
					}
				}
			});
		}
		// Set
		aggregated.setData(listData);
		aggregated.setResults(totalResults);
		logger.println(LocalMessages.ANALYZE.toString() + " " + LocalMessages.FINISHED.toString());
		return aggregated;
	}
}
