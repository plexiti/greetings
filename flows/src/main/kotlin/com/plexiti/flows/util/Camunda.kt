package com.plexiti.flows.util;

import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
internal fun property(property: String, model: BpmnModelElementInstance): String {
    return model.domElement.childElements.find { it.localName == "extensionElements" }
        ?.childElements?.find { it.localName == "properties" }
        ?.childElements?.find { it.localName == "property" && it.hasAttribute("name") && it.getAttribute("name") == property }
        ?.getAttribute("value") ?: throw IllegalArgumentException("Property must be specified as <camunda:property name='${property}'/>)")
}
