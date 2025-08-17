package org.operaton.bpm.engine.impl.cmd;

import java.util.ArrayList;
import java.util.List;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

public class GetAvailableActivitiesInAdHocSubProcessCmd implements AdHocSubProcessCmd<List<String>> {

  private final String executionId;

  public GetAvailableActivitiesInAdHocSubProcessCmd(String executionId) {
    this.executionId = executionId;
  }

  @Override
  public List<String> execute(CommandContext commandContext) {
    ExecutionEntity execution = getExecutionEntity(commandContext, executionId);

    List<String> availableActivityIds = new ArrayList<>();

    // Get the current activity
    ActivityImpl currentActivity = execution.getActivity();
    if (currentActivity != null) {
      // Check if we're in an ad-hoc subprocess
      ScopeImpl parentScope = currentActivity.getFlowScope();
      if (parentScope instanceof ActivityImpl parentActivity && isAdHocSubProcess(parentActivity)) {
        // Find all activities within the ad-hoc subprocess
        collectAvailableActivities(parentActivity, availableActivityIds);
      } else if (isAdHocSubProcess(currentActivity)) {
        // Current activity is the ad-hoc subprocess itself
        collectAvailableActivities(currentActivity, availableActivityIds);
      }
    }

    return availableActivityIds;
  }
}
