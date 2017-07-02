package com.plexiti.commons.application

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.camunda.bpm.engine.RuntimeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
class FlowService {

    @Autowired
    lateinit var runtimeService: RuntimeService

    @Transactional
    fun executeCommand(json: String) {
        val tokenId = tokenId(json)
        if (tokenId != null) {
            runtimeService.signal(tokenId.value, null, org.camunda.spin.Spin.JSON(json), null)
        }
    }

    private fun tokenId(json: String): TokenId? {
        try {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val id =  node.get("tokenId").textValue()
            return if (id != null) TokenId(id) else null
        } catch (ex: JsonMappingException) {
            return null
        }
    }

}
