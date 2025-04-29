package org.operaton.bpm.engine.test.util;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.ThreadPoolJobExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;


/**
 * Utility class providing helper methods to manage job execution and job executor operations
 * in a process engine environment.
 * This class includes methods for waiting for job executors to process all jobs,
 * monitoring job availability, and ensuring completion of job executor tasks.
 * It also provides mechanisms for condition-based waiting within specified time limits.
 * This class is designed to support process engine testing and maintenance operations.
 */
public class JobExecutorHelper {

    public static final long CHECK_INTERVAL_MS = 1000L;
    public static final long JOBS_WAIT_TIMEOUT_MS = 20_000L;
    private static final int JOB_EXECUTOR_WAIT_MULTIPLIER = 2;
    private static final int THREAD_POOL_ACTIVE_COUNT_ZERO = 0;

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

        int jobExecutorWaitTime = jobExecutor.getWaitTimeInMillis() * JOB_EXECUTOR_WAIT_MULTIPLIER;
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
            throw new ProcessEngineException("Time limit of " + maxMillisToWait + " was exceeded (still " + numberOfJobsAvailable(managementService) + " jobs available)", e);
        } finally {
            if (shutdown) {
                jobExecutor.shutdown();
            }
        }
    }

    public static void waitForJobExecutionRunnablesToFinish(long maxMillisToWait, long intervalMillis, JobExecutor jobExecutor) {
        waitForCondition(() -> ((ThreadPoolJobExecutor) jobExecutor).getThreadPoolExecutor().getActiveCount() == THREAD_POOL_ACTIVE_COUNT_ZERO, maxMillisToWait, intervalMillis);
    }

    public static void waitForCondition(Callable<Boolean> condition, long maxMillisToWait, long checkInterval) {
        try {
            await()
                    .atMost(maxMillisToWait, TimeUnit.MILLISECONDS)
                    .pollInterval(checkInterval, TimeUnit.MILLISECONDS)
                    .ignoreExceptions() // In case condition throws an exception during polling
                    .until(condition);
        } catch (Exception e) {
            throw new ProcessEngineException("Time limit of " + maxMillisToWait + " was exceeded.");
        }
    }

    public static boolean areJobsAvailable(ManagementService managementService) {
        return numberOfJobsAvailable(managementService) > 0;      // Check if there are any matching jobs
    }

    public static long numberOfJobsAvailable(ManagementService managementService) {
        // Directly count the number of jobs that are available using the JobQuery interface
        return managementService.createJobQuery()
                .withRetriesLeft() // Select jobs with retries > 0
                .executable()      // Ensure jobs are due (null or past due date)
                .count();          // Efficiently count the jobs in the database
    }

    private static ManagementService getManagementService(ProcessEngineConfigurationImpl processEngineConfiguration) {
        return processEngineConfiguration.getManagementService();
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineService processEngineService = BpmPlatform.getProcessEngineService();
        return processEngineService.getDefaultProcessEngine();
    }

}
