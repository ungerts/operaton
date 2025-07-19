package org.operaton.bpm.engine.test.bpmn.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

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

  RepositoryService repositoryService;
  RuntimeService runtimeService;

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

  private String deployAdHocSubProcessModel() {
    String processDefinitionKey = "adHocSubProcessTest";

    // Create a simple BPMN model with an ad-hoc subprocess
    String bpmnModel = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
        "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "             targetNamespace=\"http://operaton.org/examples\"\n" +
        "             xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\">\n" +
        "\n" +
        "  <process id=\"" + processDefinitionKey + "\" isExecutable=\"true\">\n" +
        "    <startEvent id=\"start\"/>\n" +
        "    <sequenceFlow id=\"flow1\" sourceRef=\"start\" targetRef=\"adHocSubProcess\"/>\n" +
        "    \n" +
        "    <adHocSubProcess id=\"adHocSubProcess\" ordering=\"Parallel\" cancelRemainingInstances=\"true\">\n" +
        "      <startEvent id=\"subStart\"/>\n" +
        "      <sequenceFlow id=\"subFlow1\" sourceRef=\"subStart\" targetRef=\"userTask\"/>\n" +
        "      <userTask id=\"userTask\" name=\"Ad-hoc Task\"/>\n" +
        "      <sequenceFlow id=\"subFlow2\" sourceRef=\"userTask\" targetRef=\"subEnd\"/>\n" +
        "      <endEvent id=\"subEnd\"/>\n" +
        "      <completionCondition>true</completionCondition>\n" +
        "    </adHocSubProcess>\n" +
        "    \n" +
        "    <sequenceFlow id=\"flow2\" sourceRef=\"adHocSubProcess\" targetRef=\"end\"/>\n" +
        "    <endEvent id=\"end\"/>\n" +
        "  </process>\n" +
        "</definitions>";

    // Deploy the model
    repositoryService.createDeployment()
        .addString(processDefinitionKey + ".bpmn", bpmnModel)
        .deploy();

    return processDefinitionKey;
  }
}
