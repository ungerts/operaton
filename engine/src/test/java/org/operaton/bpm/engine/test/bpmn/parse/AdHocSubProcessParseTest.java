package org.operaton.bpm.engine.test.bpmn.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * Test case for Ad-hoc Sub Process parsing validation.
 */
class AdHocSubProcessParseTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @BeforeEach
  void setup() {
    repositoryService = engineRule.getRepositoryService();
    runtimeService = engineRule.getRuntimeService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @AfterEach
  void tearDown() {
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testValidAdHocSubProcessParsing() {
    // Deploy a valid BPMN process with an ad-hoc subprocess
    String bpmnModel = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                          xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                          xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                          id="Definitions_ValidAdHoc"
                          targetNamespace="http://bpmn.io/schema/bpmn">

          <bpmn:process id="ValidAdHocProcess" name="Valid Ad-hoc Process" isExecutable="true">
            <bpmn:startEvent id="StartEvent_1">
              <bpmn:outgoing>Flow_ToAdHoc</bpmn:outgoing>
            </bpmn:startEvent>

            <bpmn:adHocSubProcess id="AdHoc_Valid" name="Valid Ad-hoc SubProcess">
              <bpmn:incoming>Flow_ToAdHoc</bpmn:incoming>
              <bpmn:outgoing>Flow_ToEnd</bpmn:outgoing>

              <bpmn:userTask id="Task1" name="Task 1" />
              <bpmn:userTask id="Task2" name="Task 2" />
            </bpmn:adHocSubProcess>

            <bpmn:sequenceFlow id="Flow_ToAdHoc" sourceRef="StartEvent_1" targetRef="AdHoc_Valid" />
            <bpmn:endEvent id="EndEvent_1">
              <bpmn:incoming>Flow_ToEnd</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_ToEnd" sourceRef="AdHoc_Valid" targetRef="EndEvent_1" />
          </bpmn:process>

        </bpmn:definitions>
        """;

    // Deploy the model
    repositoryService.createDeployment()
        .addString("validAdHocProcess.bpmn", bpmnModel)
        .deploy();

    // Verify that the process definition was deployed successfully
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ValidAdHocProcess")
        .singleResult();

    assertThat(processDefinition).as("Process definition should be deployed").isNotNull();
    assertThat(processDefinition.getKey()).as("Process definition key should match").isEqualTo("ValidAdHocProcess");
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    assertThat(processInstance).as("Process instance should be created").isNotNull();
    assertThat(processInstance.isEnded()).as("Process instance should not be ended").isFalse();
    Execution execution = runtimeService.createExecutionQuery().activityId("AdHoc_Valid").singleResult();
    assertThat(execution).as("Execution for ad-hoc subprocess should exist").isNotNull();
    List<String> activityIds = runtimeService.getAvailableActivitiesInAdHocSubProcess(execution.getId());
    assertThat(activityIds).as("Available activities in ad-hoc subprocess should match").containsExactlyInAnyOrder("Task1", "Task2");
  }

  @Test
  void testAdHocSubProcessWithInvalidStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testAdHocSubProcessWithInvalidStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    ParseException exception = assertThrows(ParseException.class, deploymentBuilder::deploy, "Exception expected: Ad-hoc subprocess should not contain start events.");

    testRule.assertTextPresent("Ad-hoc subprocess cannot contain start events", exception.getMessage());
    List<Problem> errors = exception.getResourceReports().get(0).getErrors();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).getMainElementId()).isEqualTo("AdHoc_WithInvalidStartEvent");
  }

  @Test
  void testAdHocSubProcessWithInvalidEndEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testAdHocSubProcessWithInvalidEndEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    ParseException exception = assertThrows(ParseException.class, deploymentBuilder::deploy, "Exception expected: Ad-hoc subprocess should not contain end events.");

    testRule.assertTextPresent("Ad-hoc subprocess cannot contain end events", exception.getMessage());
    List<Problem> errors = exception.getResourceReports().get(0).getErrors();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).getMainElementId()).isEqualTo("AdHoc_WithInvalidEndEvent");
  }

  @Test
  void testAdHocSubProcessWithSequenceFlows() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testAdHocSubProcessWithSequenceFlows");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    ParseException exception = assertThrows(ParseException.class, deploymentBuilder::deploy, "Exception expected: Ad-hoc subprocess should not contain sequence flows between activities.");

    testRule.assertTextPresent("ad-hoc subprocess must not contain sequence flows", exception.getMessage());
    List<Problem> errors = exception.getResourceReports().get(0).getErrors();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).getMainElementId()).isEqualTo("SequenceFlow_InAdHoc");
  }

  @Test
  void testEmptyAdHocSubProcess() {
    String bpmnModel = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          id="Definitions_EmptyAdHoc"
                          targetNamespace="http://bpmn.io/schema/bpmn">

          <bpmn:process id="EmptyAdHocProcess" isExecutable="true">
            <bpmn:startEvent id="StartEvent_1">
              <bpmn:outgoing>Flow_ToAdHoc</bpmn:outgoing>
            </bpmn:startEvent>

            <bpmn:adHocSubProcess id="AdHoc_Empty" name="Empty Ad-hoc SubProcess">
              <bpmn:incoming>Flow_ToAdHoc</bpmn:incoming>
              <bpmn:outgoing>Flow_ToEnd</bpmn:outgoing>
            </bpmn:adHocSubProcess>

            <bpmn:sequenceFlow id="Flow_ToAdHoc" sourceRef="StartEvent_1" targetRef="AdHoc_Empty" />
            <bpmn:endEvent id="EndEvent_1">
              <bpmn:incoming>Flow_ToEnd</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_ToEnd" sourceRef="AdHoc_Empty" targetRef="EndEvent_1" />
          </bpmn:process>

        </bpmn:definitions>
        """;

    var deploymentBuilder = repositoryService.createDeployment().addString("emptyAdHocProcess.bpmn", bpmnModel);

    ParseException exception = assertThrows(ParseException.class, deploymentBuilder::deploy, "Exception expected: Empty ad-hoc subprocess should not be allowed.");

    testRule.assertTextPresent("ad-hoc subprocess must contain at least one activity", exception.getMessage());
    List<Problem> errors = exception.getResourceReports().get(0).getErrors();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).getMainElementId()).isEqualTo("AdHoc_Empty");
  }

  @Test
  @Deployment
  void testValidAdHocSubProcessWithMultipleTasks() {
    // Test is defined by @Deployment annotation
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .singleResult();

    assertThat(processDefinition).isNotNull();
    assertThat(processDefinition.getKey()).isEqualTo("MultiTaskAdHocProcess");
  }

  @Test
  void testAdHocSubProcessWithInvalidGateway() {
    String bpmnModel = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          id="Definitions_InvalidGateway"
                          targetNamespace="http://bpmn.io/schema/bpmn">

          <bpmn:process id="InvalidGatewayAdHocProcess" isExecutable="true">
            <bpmn:startEvent id="StartEvent_1">
              <bpmn:outgoing>Flow_ToAdHoc</bpmn:outgoing>
            </bpmn:startEvent>

            <bpmn:adHocSubProcess id="AdHoc_InvalidGateway" name="Ad-hoc with Invalid Gateway">
              <bpmn:incoming>Flow_ToAdHoc</bpmn:incoming>
              <bpmn:outgoing>Flow_ToEnd</bpmn:outgoing>

              <bpmn:userTask id="Task1" name="Task 1" />
              <bpmn:exclusiveGateway id="Gateway_Invalid" name="Invalid Gateway">
                <bpmn:incoming>Flow_Task1ToGateway</bpmn:incoming>
                <bpmn:outgoing>Flow_GatewayToTask2</bpmn:outgoing>
              </bpmn:exclusiveGateway>
              <bpmn:userTask id="Task2" name="Task 2" />
              
              <bpmn:sequenceFlow id="Flow_Task1ToGateway" sourceRef="Task1" targetRef="Gateway_Invalid" />
              <bpmn:sequenceFlow id="Flow_GatewayToTask2" sourceRef="Gateway_Invalid" targetRef="Task2" />
            </bpmn:adHocSubProcess>

            <bpmn:sequenceFlow id="Flow_ToAdHoc" sourceRef="StartEvent_1" targetRef="AdHoc_InvalidGateway" />
            <bpmn:endEvent id="EndEvent_1">
              <bpmn:incoming>Flow_ToEnd</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_ToEnd" sourceRef="AdHoc_InvalidGateway" targetRef="EndEvent_1" />
          </bpmn:process>

        </bpmn:definitions>
        """;

    var deploymentBuilder = repositoryService.createDeployment().addString("invalidGatewayAdHocProcess.bpmn", bpmnModel);

    ParseException exception = assertThrows(ParseException.class, deploymentBuilder::deploy, "Exception expected: Ad-hoc subprocess should not contain gateways with sequence flows.");

    testRule.assertTextPresent("ad-hoc subprocess must not contain sequence flows", exception.getMessage());
    List<Problem> errors = exception.getResourceReports().get(0).getErrors();
    assertThat(errors).hasSizeGreaterThan(0);
  }

  @Test
  void testNestedAdHocSubProcess() {
    String bpmnModel = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          id="Definitions_NestedAdHoc"
                          targetNamespace="http://bpmn.io/schema/bpmn">

          <bpmn:process id="NestedAdHocProcess" isExecutable="true">
            <bpmn:startEvent id="StartEvent_1">
              <bpmn:outgoing>Flow_ToAdHoc</bpmn:outgoing>
            </bpmn:startEvent>

            <bpmn:adHocSubProcess id="AdHoc_Outer" name="Outer Ad-hoc SubProcess">
              <bpmn:incoming>Flow_ToAdHoc</bpmn:incoming>
              <bpmn:outgoing>Flow_ToEnd</bpmn:outgoing>

              <bpmn:userTask id="Task1" name="Task 1" />
              
              <bpmn:adHocSubProcess id="AdHoc_Inner" name="Inner Ad-hoc SubProcess">
                <bpmn:userTask id="Task2" name="Task 2" />
                <bpmn:userTask id="Task3" name="Task 3" />
              </bpmn:adHocSubProcess>
            </bpmn:adHocSubProcess>

            <bpmn:sequenceFlow id="Flow_ToAdHoc" sourceRef="StartEvent_1" targetRef="AdHoc_Outer" />
            <bpmn:endEvent id="EndEvent_1">
              <bpmn:incoming>Flow_ToEnd</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_ToEnd" sourceRef="AdHoc_Outer" targetRef="EndEvent_1" />
          </bpmn:process>

        </bpmn:definitions>
        """;

    // Deploy the model - nested ad-hoc subprocesses should be allowed
    repositoryService.createDeployment()
        .addString("nestedAdHocProcess.bpmn", bpmnModel)
        .deploy();

    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("NestedAdHocProcess")
        .singleResult();

    assertThat(processDefinition).isNotNull();
  }

  @Test
  void testAdHocSubProcessWithCompletionCondition() {
    String bpmnModel = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          id="Definitions_CompletionCondition"
                          targetNamespace="http://bpmn.io/schema/bpmn">

          <bpmn:process id="CompletionConditionAdHocProcess" isExecutable="true">
            <bpmn:startEvent id="StartEvent_1">
              <bpmn:outgoing>Flow_ToAdHoc</bpmn:outgoing>
            </bpmn:startEvent>

            <bpmn:adHocSubProcess id="AdHoc_WithCompletion" name="Ad-hoc with Completion Condition" cancelRemainingInstances="false">
              <bpmn:incoming>Flow_ToAdHoc</bpmn:incoming>
              <bpmn:outgoing>Flow_ToEnd</bpmn:outgoing>

              <bpmn:completionCondition><![CDATA[${completed == true}]]></bpmn:completionCondition>

              <bpmn:userTask id="Task1" name="Task 1" />
              <bpmn:userTask id="Task2" name="Task 2" />
              <bpmn:userTask id="Task3" name="Task 3" />
            </bpmn:adHocSubProcess>

            <bpmn:sequenceFlow id="Flow_ToAdHoc" sourceRef="StartEvent_1" targetRef="AdHoc_WithCompletion" />
            <bpmn:endEvent id="EndEvent_1">
              <bpmn:incoming>Flow_ToEnd</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_ToEnd" sourceRef="AdHoc_WithCompletion" targetRef="EndEvent_1" />
          </bpmn:process>

        </bpmn:definitions>
        """;

    // Deploy the model
    repositoryService.createDeployment()
        .addString("completionConditionAdHocProcess.bpmn", bpmnModel)
        .deploy();

    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("CompletionConditionAdHocProcess")
        .singleResult();

    assertThat(processDefinition).isNotNull();
  }

}
