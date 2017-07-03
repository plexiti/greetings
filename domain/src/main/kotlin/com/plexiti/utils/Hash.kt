package com.plexiti.utils

import org.apache.commons.codec.digest.DigestUtils

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
fun hash(text: String): String {

    return DigestUtils.shaHex(text)

}
