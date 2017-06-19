package com.plexiti.commons.domain

import org.assertj.core.api.Assertions.*
import org.junit.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventTest: DataJpaTest() {

    @Test fun empty () {
        assertThat(eventRepository.findAll()).isEmpty()
    }

}
