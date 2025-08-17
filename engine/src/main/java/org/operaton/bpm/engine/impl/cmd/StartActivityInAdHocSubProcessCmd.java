package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

/**
 * This class is responsible for starting an activity within an ad-hoc subprocess.
 * It implements the {@link AdHocSubProcessCmd} interface and performs validations
 * and execution of a specific activity within an ad-hoc subprocess, given the
 * execution ID and the target activity ID.
 * <p>
 * The class ensures that the execution is currently within an ad-hoc subprocess
 * and validates the presence of the target activity ID within its scope. Upon validation,
 * it creates a new execution for the specified activity and starts it.
 *
 * @author Tobias Unger
 */
public class StartActivityInAdHocSubProcessCmd implements AdHocSubProcessCmd<Void> {
  private final String executionId;
  private final String activityId;

  public StartActivityInAdHocSubProcessCmd(String executionId, String activityId) {
    this.executionId = executionId;
    this.activityId = activityId;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    EnsureUtil.ensureNotNull("activityId", activityId);
    ExecutionEntity execution = getExecutionEntity(commandContext, executionId);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      // TODO: Discuss if we need to check for ad-hoc subprocess permissions
      checker.checkCreateProcessInstance(execution.getProcessDefinition());
    }
    
    ActivityImpl adHocSubProcess = validateAndGetAdHocSubProcess(execution);
    validateAdHocSubProcessOrdering(adHocSubProcess, execution);
    ActivityImpl targetActivity = validateAndGetTargetActivity(adHocSubProcess);
    createAndStartActivity(execution, targetActivity);

    return null;
  }

  private ActivityImpl validateAndGetAdHocSubProcess(ExecutionEntity execution) {
    ActivityImpl executionActivity = execution.getActivity();
    if (executionActivity == null || !isAdHocSubProcess(executionActivity)) {
      throw new NotValidException(
          "The execution is not in an ad-hoc subprocess or the current activity is not an ad-hoc subprocess.");
    }
    return executionActivity;
  }

  private ActivityImpl validateAndGetTargetActivity(ActivityImpl adHocSubProcess) {
    ActivityImpl targetActivity = findActivityById(adHocSubProcess, activityId);
    if (targetActivity == null) {
      throw new NotValidException("Activity with id '" + activityId + "' not found in ad-hoc subprocess");
    }
    return targetActivity;
  }

  private void createAndStartActivity(ExecutionEntity execution, ActivityImpl targetActivity) {
    ExecutionEntity newExecution = execution.createExecution();
    newExecution.setActivity(targetActivity);
    newExecution.setActive(true);
    newExecution.executeActivity(targetActivity);
  }

  private ActivityImpl findActivityById(ActivityImpl adHocSubProcess, String activityId) {
    return adHocSubProcess.getActivities() == null ?
        null :
        adHocSubProcess.getActivities()
            .stream()
            .filter(activity -> activityId.equals(activity.getActivityId()))
            .findFirst()
            .orElse(null);
  }

  private void validateAdHocSubProcessOrdering(ActivityImpl adHocSubProcess, ExecutionEntity execution) {
    String ordering = getAdHocSubProcessOrdering(adHocSubProcess);

    if ("Sequential".equals(ordering)) {
      // In a sequential ad-hoc subprocess, check if there are already active executions
      // Only one activity can be active at a time
      if (hasActiveChildExecutions(execution)) {
        throw new NotValidException(
            "Cannot start activity '" + activityId + "' in sequential ad-hoc subprocess. " +
            "Another activity is already running.");
      }
    }
    // For parallel ordering, multiple activities can run simultaneously
    // No additional validation needed
  }

  private String getAdHocSubProcessOrdering(ActivityImpl adHocSubProcess) {
    // The ordering property is stored in the activity properties
    // TODO: Use BpmnProperties instead of hardcoded property names
    Object ordering = adHocSubProcess.getProperty("ordering");
    if (ordering != null) {
      return ordering.toString();
    }
    // Default to parallel if not specified (BPMN 2.0 default)
    return "Parallel";
  }

  private boolean hasActiveChildExecutions(ExecutionEntity execution) {
    // Check if there are any active child executions in the ad-hoc subprocess
    return execution.getExecutions() != null &&
           execution.getExecutions().stream()
               .anyMatch(childExecution -> childExecution.isActive() && !childExecution.isEnded());
  }
}