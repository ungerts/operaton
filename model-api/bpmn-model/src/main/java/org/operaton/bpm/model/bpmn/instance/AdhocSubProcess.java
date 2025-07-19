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
package org.operaton.bpm.model.bpmn.instance;

/**
 * The BPMN adHocSubProcess element
 *
 * @author OIP-003 Implementation
 */
public interface AdhocSubProcess extends SubProcess {

  /**
   * Gets the ordering of activities within the adhoc subprocess.
   * Can be "Sequential" or "Parallel".
   *
   * @return the ordering type
   */
  String getOrdering();

  /**
   * Sets the ordering of activities within the adhoc subprocess.
   *
   * @param ordering the ordering type ("Sequential" or "Parallel")
   */
  void setOrdering(String ordering);

  /**
   * Gets the completion condition expression for the adhoc subprocess.
   * This determines when the subprocess should complete.
   *
   * @return the completion condition expression
   */
  String getCompletionCondition();

  /**
   * Sets the completion condition expression for the adhoc subprocess.
   *
   * @param completionCondition the completion condition expression
   */
  void setCompletionCondition(String completionCondition);

  /**
   * Gets whether remaining instances should be canceled when the
   * completion condition is met.
   *
   * @return true if remaining instances should be canceled
   */
  boolean getCancelRemainingInstances();

  /**
   * Sets whether remaining instances should be canceled when the
   * completion condition is met.
   *
   * @param cancelRemainingInstances true to cancel remaining instances
   */
  void setCancelRemainingInstances(boolean cancelRemainingInstances);
}
