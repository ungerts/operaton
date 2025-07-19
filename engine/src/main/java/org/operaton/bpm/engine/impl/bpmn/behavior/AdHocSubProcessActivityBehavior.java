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

import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;

/**
 * Implementation of the Ad-hoc Sub Process Activity Behavior.
 *
 * An ad-hoc sub process is a sub process that is executed in an ad-hoc manner,
 * meaning that its activities can be executed in any order and multiple times.
 * The completion of an ad-hoc sub process is determined by a completion condition.
 *
 * @author GitHub Copilot
 */
public class AdHocSubProcessActivityBehavior extends SubProcessActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    // Execute the ad-hoc sub process logic using standard subprocess behavior
    super.execute(execution);
  }

  @Override
  public void leave(ActivityExecution execution) {
    // Check completion condition before leaving
    String completionCondition = (String) execution.getActivity().getProperty("completionCondition");

    if (completionCondition != null && !completionCondition.trim().isEmpty()) {
      // Evaluate completion condition
      // If condition is met, proceed with leaving
      if (evaluateCompletionCondition(execution, completionCondition)) {
        super.leave(execution);
      }
      // Otherwise, stay in the ad-hoc sub process
    } else {
      // No completion condition specified, use default behavior
      super.leave(execution);
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
    // In a full implementation, this would evaluate the expression using the expression manager
    // For example: expressionManager.createExpression(completionCondition).getValue(execution)
    return true;
  }
}
