package org.operaton.bpm.engine.impl.cmd;

import java.util.List;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

public interface AdHocSubProcessCmd<T> extends Command<T> {

  default boolean isAdHocSubProcess(ActivityImpl activity) {
    return activity != null &&
        "adHocSubProcess".equals(activity.getProperty("type")) &&
        // TODO: maybe remove this check in the future (also in BpmnParse). type should be enough
        Boolean.TRUE.equals(activity.getProperty("isAdHoc"));
  }

  default void collectAvailableActivities(ActivityImpl adHocSubProcess, List<String> activityIds) {
    // TODO: check which activities are available for start based on the ad-hoc subprocess rules
    if (adHocSubProcess.getActivities() != null) {
      for (ActivityImpl childActivity : adHocSubProcess.getActivities()) {
        if (childActivity.getActivityId() != null) {
          activityIds.add(childActivity.getActivityId());
        }
      }
    }
  }

  default ExecutionEntity getExecutionEntity(CommandContext commandContext, String executionId) {
    EnsureUtil.ensureNotNull("executionId", executionId);

    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(executionId);
    EnsureUtil.ensureNotNull("execution", execution);
    return execution;
  }

}
