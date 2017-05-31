package com.plexiti.greetings

import org.apache.camel.Handler
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class GreetingsReader {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Handler
    fun read(stream: InputStream) {
        logger.info("File changed.")
        IOUtils.copy(stream, System.out)
    }

}
