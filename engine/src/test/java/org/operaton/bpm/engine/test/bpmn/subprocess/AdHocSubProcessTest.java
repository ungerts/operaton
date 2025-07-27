package org.operaton.bpm.engine.test.bpmn.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * Test case for Ad-hoc Sub Process parsing and execution.
 */
class AdHocSubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  static RepositoryService repositoryService;
  RuntimeService runtimeService;

  @BeforeAll
  static void setUpOnce() {
    repositoryService = engineRule.getRepositoryService();
    deployAdHocSubProcessModel();
  }

  @Test
  void testAdHocSubProcessParsing() {
    // Deploy a simple BPMN process with an ad-hoc subprocess
    String processDefinitionKey = deployAdHocSubProcessModel();

    // Verify that the process definition was deployed successfully
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .singleResult();

    assertThat(processDefinition).as("Process definition should be deployed").isNotNull();
    assertThat(processDefinition.getKey()).as("Process definition key should match").isEqualTo(processDefinitionKey);
  }

  @Test
  void testAdHocSubProcessExecution() {
    // Deploy the process
    String processDefinitionKey = deployAdHocSubProcessModel();

    // Start a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

    assertThat(processInstance).as("Process instance should be created").isNotNull();
    assertThat(processInstance.isEnded()).as("Process instance should not be ended").isFalse();
  }

  private static String deployAdHocSubProcessModel() {
    String processDefinitionKey = "ExpenseApprovalProcess";

    // Create a BPMN model with an ad-hoc subprocess for expense approval
    String bpmnModel = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                          xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                          xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                          xmlns:modeler="http://camunda.org/schema/modeler/1.0"
                          id="Definitions_ExpenseApproval"
                          targetNamespace="http://bpmn.io/schema/bpmn"
                          exporter="Camunda Modeler"
                          exporterVersion="5.34.0"
                          modeler:executionPlatform="Camunda Platform"
                          modeler:executionPlatformVersion="7.23.0">

          <bpmn:process id="ExpenseApprovalProcess" name="Employee Expense Approval" isExecutable="true">
            <bpmn:startEvent id="StartEvent_EmployeeRequest">
              <bpmn:outgoing>Flow_ToAdHoc</bpmn:outgoing>
            </bpmn:startEvent>

            <bpmn:adHocSubProcess id="AdHoc_ExpenseHandling" name="Handle Expense Request">
              <bpmn:incoming>Flow_ToAdHoc</bpmn:incoming>
              <bpmn:outgoing>Flow_ToEnd</bpmn:outgoing>

              <bpmn:userTask id="SubmitExpenseReport" name="Submit Expense Report" />
              <bpmn:userTask id="ReviewExpenseReport" name="Review Expense Report" />
            </bpmn:adHocSubProcess>

            <bpmn:sequenceFlow id="Flow_ToAdHoc" sourceRef="StartEvent_EmployeeRequest" targetRef="AdHoc_ExpenseHandling" />
            <bpmn:endEvent id="EndEvent_ProcessComplete">
              <bpmn:incoming>Flow_ToEnd</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_ToEnd" sourceRef="AdHoc_ExpenseHandling" targetRef="EndEvent_ProcessComplete" />
          </bpmn:process>

          <bpmndi:BPMNDiagram id="BPMNDiagram_Expense">
            <bpmndi:BPMNPlane id="BPMNPlane_Expense" bpmnElement="ExpenseApprovalProcess">
              <bpmndi:BPMNShape id="StartEvent_EmployeeRequest_di" bpmnElement="StartEvent_EmployeeRequest">
                <dc:Bounds x="152" y="202" width="36" height="36" />
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="EndEvent_ProcessComplete_di" bpmnElement="EndEvent_ProcessComplete">
                <dc:Bounds x="762" y="202" width="36" height="36" />
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="AdHoc_ExpenseHandling_di" bpmnElement="AdHoc_ExpenseHandling" isExpanded="true">
                <dc:Bounds x="300" y="80" width="350" height="310" />
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="SubmitExpenseReport_di" bpmnElement="SubmitExpenseReport">
                <dc:Bounds x="430" y="120" width="100" height="80" />
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="ReviewExpenseReport_di" bpmnElement="ReviewExpenseReport">
                <dc:Bounds x="430" y="260" width="100" height="80" />
              </bpmndi:BPMNShape>

              <bpmndi:BPMNEdge id="Flow_ToAdHoc_di" bpmnElement="Flow_ToAdHoc">
                <di:waypoint x="188" y="220" />
                <di:waypoint x="300" y="220" />
              </bpmndi:BPMNEdge>

              <bpmndi:BPMNEdge id="Flow_ToEnd_di" bpmnElement="Flow_ToEnd">
                <di:waypoint x="650" y="220" />
                <di:waypoint x="762" y="220" />
              </bpmndi:BPMNEdge>
            </bpmndi:BPMNPlane>
          </bpmndi:BPMNDiagram>
        </bpmn:definitions>
        """;

    // Deploy the model
    repositoryService.createDeployment()
        .addString(processDefinitionKey + ".bpmn", bpmnModel)
        .deploy();

    return processDefinitionKey;
  }
}
