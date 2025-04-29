package org.operaton.bpm.engine.test.util;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.ThreadPoolJobExecutor;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * A utility helper class designed to facilitate job execution operations, particularly for
 * controlling and monitoring the behavior of the {@link JobExecutor}. It provides methods
 * to wait for job executions, check job availability, and handle timeout conditions.
 * This class contains static utility methods and is not intended to be instantiated.
 */
public class JobExecutorHelper {

    public static final long CHECK_INTERVAL_MS = 1000L;
    protected static final long JOBS_WAIT_TIMEOUT_MS = 20_000L;

    private JobExecutorHelper() {
        // Private constructor to prevent instantiation
    }


    public static void waitForJobExecutorToProcessAllJobs() {
        waitForJobExecutorToProcessAllJobs(JOBS_WAIT_TIMEOUT_MS);
    }


    public static void waitForJobExecutorToProcessAllJobs(long maxMillisToWait) {
        // Get the process engine instance (previously done before each test run)
        ProcessEngine processEngine = getProcessEngine();

        // Retrieve the job executor from the process engine configuration
        ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();
        waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait);
    }

    public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfigurationImpl processEngineConfiguration, long maxMillisToWait) {
        // Check interval configuration (deprecated and unused prior to migration)

        waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait, CHECK_INTERVAL_MS);
    }

    public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfigurationImpl processEngineConfiguration, long maxMillisToWait, long checkInterval) {
        JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
        ManagementService managementService = getManagementService(processEngineConfiguration);
        // Ensure the job executor is inactive before proceeding
        if (!jobExecutor.isActive()) {
            jobExecutor.start();
        }

        jobExecutor.start();
        int jobExecutorWaitTime = jobExecutor.getWaitTimeInMillis() * 2;
        if (maxMillisToWait < jobExecutorWaitTime) {
            maxMillisToWait = jobExecutorWaitTime;
        }
        boolean shutdown = true;
        waitForJobExecutorToProcessAllJobs(maxMillisToWait, checkInterval, jobExecutor,managementService, shutdown);
    }

    public static void waitForJobExecutorToProcessAllJobs(long maxMillisToWait, long checkInterval, JobExecutor jobExecutor,ManagementService managementService, boolean shutdown) {
        try {
            Callable<Boolean> condition = () -> !areJobsAvailable(managementService);
            waitForCondition(condition,maxMillisToWait, checkInterval);
        } catch (Exception e) {
            throw new IllegalStateException("Time limit of " + maxMillisToWait + " was exceeded (still " + numberOfJobsAvailable(managementService) + " jobs available)", e);
        } finally {
            if (shutdown) {
                jobExecutor.shutdown();
            }
        }
    }

    public static void waitForJobExecutionRunnablesToFinish(long maxMillisToWait, long intervalMillis, JobExecutor jobExecutor) {
        waitForCondition(() -> ((ThreadPoolJobExecutor) jobExecutor).getThreadPoolExecutor().getActiveCount() == 0, maxMillisToWait, intervalMillis);
    }

    public static void waitForCondition(Callable<Boolean> condition, long maxMillisToWait, long checkInterval) {
        try {
            await()
                    .atMost(maxMillisToWait, TimeUnit.MILLISECONDS)
                    .pollInterval(checkInterval, TimeUnit.MILLISECONDS)
                    .ignoreExceptions() // In case condition throws an exception during polling
                    .until(condition);
        } catch (Exception e) {
            throw new IllegalStateException("Time limit of " + maxMillisToWait + " was exceeded.");
        }
    }

    public static boolean areJobsAvailable(ManagementService managementService) {
        List<Job> list = managementService.createJobQuery().list();
        for (Job job : list) {
            if (isJobAvailable(job)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isJobAvailable(Job job) {
        return job.getRetries() > 0 && (job.getDuedate() == null || ClockUtil.getCurrentTime().after(job.getDuedate()));
    }

    public static int numberOfJobsAvailable(ManagementService managementService) {
        int numberOfJobs = 0;
        List<Job> jobs = managementService.createJobQuery().list();
        for (Job job : jobs) {
            if (isJobAvailable(job)) {
                numberOfJobs++;
            }
        }
        return numberOfJobs;
    }

    private static ManagementService getManagementService(ProcessEngineConfigurationImpl processEngineConfiguration) {
        return processEngineConfiguration.getManagementService();
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineService processEngineService = BpmPlatform.getProcessEngineService();
        return processEngineService.getDefaultProcessEngine();
    }

}
