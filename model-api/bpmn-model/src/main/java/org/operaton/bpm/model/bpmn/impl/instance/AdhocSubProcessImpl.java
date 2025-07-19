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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.instance.AdhocSubProcess;
import org.operaton.bpm.model.bpmn.instance.CompletionCondition;
import org.operaton.bpm.model.bpmn.instance.SubProcess;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN adHocSubProcess element implementation
 *
 * @author OIP-003 Implementation
 */
public class AdhocSubProcessImpl extends SubProcessImpl implements AdhocSubProcess {

  protected static Attribute<String> orderingAttribute;
  protected static Attribute<Boolean> cancelRemainingInstancesAttribute;
  protected static ChildElement<CompletionCondition> completionConditionChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(AdhocSubProcess.class, BPMN_ELEMENT_AD_HOC_SUB_PROCESS)
      .namespaceUri(BPMN20_NS)
      .extendsType(SubProcess.class)
      .instanceProvider(AdhocSubProcessImpl::new);

    orderingAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ORDERING)
      .defaultValue("Parallel")
      .build();

    cancelRemainingInstancesAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_CANCEL_REMAINING_INSTANCES)
      .defaultValue(true)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    completionConditionChild = sequenceBuilder.element(CompletionCondition.class)
      .build();

    typeBuilder.build();
  }

  public AdhocSubProcessImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public String getOrdering() {
    return orderingAttribute.getValue(this);
  }

  @Override
  public void setOrdering(String ordering) {
    orderingAttribute.setValue(this, ordering);
  }

  @Override
  public String getCompletionCondition() {
    CompletionCondition completionCondition = completionConditionChild.getChild(this);
    if (completionCondition != null) {
      return completionCondition.getTextContent();
    }
    return null;
  }

  @Override
  public void setCompletionCondition(String completionCondition) {
    CompletionCondition completionConditionElement = completionConditionChild.getChild(this);
    if (completionConditionElement == null) {
      completionConditionElement = modelInstance.newInstance(CompletionCondition.class);
      completionConditionChild.setChild(this, completionConditionElement);
    }
    completionConditionElement.setTextContent(completionCondition);
  }

  @Override
  public boolean getCancelRemainingInstances() {
    return cancelRemainingInstancesAttribute.getValue(this);
  }

  @Override
  public void setCancelRemainingInstances(boolean cancelRemainingInstances) {
    cancelRemainingInstancesAttribute.setValue(this, cancelRemainingInstances);
  }
}
