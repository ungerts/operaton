/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.bpmn.behavior;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.bpmn.helper.CompensationUtil;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.el.Expression;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Implementation of the Ad-hoc Sub Process Activity Behavior.
 * <p>
 * An ad-hoc subprocess is a subprocess executed in an ad-hoc manner,
 * meaning that its activities can be executed in any order and multiple times.
 * The completion of an ad-hoc subprocess is determined by a completion condition.
 *
 * @author Tobias Unger
 */
public class AdHocSubProcessActivityBehavior extends SubProcessActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    // TODO: Check when we cann fallback to SubProcessActivityBehavior

    // Wrap execution with error propagation for proper BPMN error handling
    executeWithErrorPropagation(execution, () -> {
      // Execute the ad-hoc subprocess logic using standard subprocess behavior
      //super.execute(execution);
      //super.leave(execution);
      // Set the execution as a scope to allow for ad-hoc behavior
      // This is necessary to allow activities within the ad-hoc subprocess to be executed
      // No need to call super.execute(execution) here, as it is handled in the leave method
      execution.setScope(true);
      return null;
    });
  }

  @Override
  public void leave(ActivityExecution execution) {
    // Do we have to check the completion of all activities in the ad-hoc subprocess?

    try {
      // TODO: Use BpmnProperties instead of hardcoded property names
      // Check completion condition before leaving
      String completionCondition = (String) execution.getActivity().getProperty("completionCondition");

      if (completionCondition != null && !completionCondition.trim().isEmpty()) {
        // Evaluate completion condition
        // If the condition is met, proceed with leaving
        if (evaluateCompletionCondition(execution, completionCondition)) {
          // Terminate any remaining active child executions before leaving
          terminateActiveChildExecutions(execution);
          super.leave(execution);
        }
        // Otherwise, stay in the ad-hoc subprocess
      } else {
        // No completion condition specified, use default behavior
        super.leave(execution);
      }
    } catch (Exception ex) {
      // Handle exceptions during completion condition evaluation
      LOG.adHocSubprocessCompletionConditionEvaluationFailed(execution.getActivity().getId(), ex);
      throw new ProcessEngineException("Failed to evaluate completion condition for ad-hoc subprocess: " +
                                 execution.getActivity().getId(), ex);
    }
  }

  /**
   * Evaluates the completion condition for the ad-hoc sub process.
   *
   * @param execution the current execution
   * @param completionCondition the completion condition expression
   * @return true if the completion condition is met, false otherwise
   */
  protected boolean evaluateCompletionCondition(ActivityExecution execution, String completionCondition) {
    // For now, return true as a placeholder
    // In a full implementation; this would evaluate the expression using the expression manager.
    // For example, expressionManager.createExpression(completionCondition).getValue(execution)

    try {
      if (!(execution.getProcessEngine().getProcessEngineConfiguration() instanceof ProcessEngineConfigurationImpl engineConfiguration)) {
        throw new ProcessEngineException("Process Engine Configuration is not set or invalid.");
      }

      // Set local variables for the execution context. Local variables are used to provide context for the expression evaluation.
      // Local variables are not persisted, so they are only available during the execution of this method.
      execution.setVariableLocal("numberOfActiveActivities", getActiveActivitiesCount(execution));
      execution.setVariableLocal("completedActivities", getCompletedActivitiesCount(execution));
      ExpressionManager expressionManager = engineConfiguration.getExpressionManager();
      Expression expression = expressionManager.createExpression(completionCondition);

      Object result = expression.getValue(execution);

      // Convert the result to boolean
      if (result instanceof Boolean resultBoolean) {
        return resultBoolean;
      } else if (result instanceof String resultString) {
        return Boolean.parseBoolean(resultString);
      } else if (result != null) {
        throw new IllegalArgumentException("Completion condition must evaluate to a boolean value, but was: " + result);
      }
      return true; // Placeholder for default behavior
    } catch (Exception ex) {
      LOG.adHocSubprocessCompletionConditionEvaluationFailed(execution.getActivity().getId(), ex);
      throw new ProcessEngineException("Failed to evaluate completion condition expression: " + completionCondition, ex);
    }
  }

  private long getCompletedActivitiesCount(ActivityExecution execution) {
    return execution.getExecutions()
        .stream()
        .filter(childExecution -> childExecution.getActivity() != null
            && !childExecution.isActive()
            && childExecution.isEnded())
        .count();
  }

  private long getActiveActivitiesCount(ActivityExecution execution) {
    return execution.getExecutions()
        .stream()
        .filter(childExecution -> childExecution.getActivity() != null
            && childExecution.isActive()
            && !childExecution.isEnded())
        .count();
  }

  /**
   * Terminates all active child executions when the completion condition is fulfilled.
   * This is specific to ad-hoc subprocesses where activities may need to be forcibly ended.
   *
   * @param execution the ad-hoc subprocess execution
   */
  protected void terminateActiveChildExecutions(ActivityExecution execution) {
    try {
      // Get all active child executions
      List<? extends ActivityExecution> childExecutions = execution.getExecutions();
      
      for (ActivityExecution childExecution : childExecutions) {
        if (childExecution.isActive() && !childExecution.isEnded()) {
          // Log that we're terminating the activity
          LOG.adHocSubprocessTerminatingChildExecution(childExecution.getActivityInstanceId(), execution.getActivity().getId());

          // Signal the child execution to terminate gracefully if possible
          try {
            if (childExecution instanceof ExecutionEntity childEntity) {
              // Mark as ended and remove from execution tree
              childEntity.setEnded(true);
              childEntity.remove();
            } else {
              // Fallback: try to end through the execution interface
              childExecution.end(true);
            }
          } catch (Exception ex) {
            // If graceful termination fails, force removal
            LOG.adHocSubprocessGracefulTerminationFailed(childExecution.getActivityInstanceId(), ex);
            childExecution.remove();
          }
        }
      }
    } catch (Exception ex) {
      LOG.adHocSubprocessTerminationFailed(execution.getActivity().getId(), ex);
      // Don't throw here - completion should still proceed even if termination fails
    }
  }

  // Optional: allow tasks to signal the ad hoc subprocess
  @Override
  public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
    try {
      // TODI: use BpmnProperties to determine if the signal is allowed
      if ("tryComplete".equals(signalName)) {
        leave(execution);
      } else {
        super.signal(execution, signalName, signalData);
      }
    } catch (Exception ex) {
      LOG.adHocSubprocessSignalFailed(execution.getActivity().getId(), signalName, ex);
      throw ex;
    }
  }

  @Override
  public void concurrentChildExecutionEnded(ActivityExecution scopeExecution, ActivityExecution endedExecution) {
    try {
      // Handle the end of a concurrent child execution in the ad-hoc subprocess
      // This could involve checking if the completion condition is met
      if (endedExecution.isActive() || !endedExecution.isEnded()) {
        // If the ended execution is still active or not ended, we do not need to do anything
        return;
      }

      // Call parent implementation for standard cleanup
      super.concurrentChildExecutionEnded(scopeExecution, endedExecution);

      // For ad-hoc subprocesses, automatically check completion condition after child ends
      // This allows for automatic completion when conditions are met
      String completionCondition = (String) scopeExecution.getActivity().getProperty("completionCondition");
      if (completionCondition != null && !completionCondition.trim().isEmpty()) {
        if (evaluateCompletionCondition(scopeExecution, completionCondition)) {
          // Completion condition is met - terminate any remaining active activities
          terminateActiveChildExecutions(scopeExecution);
          
          // Then trigger completion
          complete(scopeExecution);
        }
      }
    } catch (Exception ex) {
      LOG.adHocSubprocessChildExecutionEndFailed(scopeExecution.getActivity().getId(), ex);
      throw new ProcessEngineException("Failed to handle concurrent child execution end in ad-hoc subprocess", ex);
    }
  }

  @Override
  public void complete(ActivityExecution scopeExecution) {
    // Handle the completion of the ad-hoc subprocess
    super.complete(scopeExecution);
  }

  @Override
  public void doLeave(ActivityExecution execution) {
    try {
      // Create event scope execution for compensation handling
      // This is crucial for ad-hoc subprocesses as activities may have been executed multiple times
      // and each execution instance needs to be compensatable

      // Ensure we have an ExecutionEntity before calling CompensationUtil
      if (execution instanceof ExecutionEntity executionEntity) {
        CompensationUtil.createEventScopeExecution(executionEntity);
      } else {
        // Log warning but continue execution - compensation might not work properly
        LOG.adHocSubprocessCompensationNotAvailable(execution.getActivity().getId(), execution.getClass().getName());
      }

      // Ensure that the execution is in a state that allows leaving the ad-hoc subprocess
      super.doLeave(execution);
    } catch (Exception ex) {
      LOG.adHocSubprocessLeaveFailed(execution.getActivity().getId(), ex);
      throw new ProcessEngineException("Failed to leave ad-hoc subprocess: " + execution.getActivity().getId(), ex);
    }
  }
}
