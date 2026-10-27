package com.example

import com.example.network.WakeOnLanManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMacAddressNormalization() {
        val mac1 = "70:85:c2:5b:14:8a"
        val normalized1 = WakeOnLanManager.normalizeMacAddress(mac1)
        assertEquals("70:85:C2:5B:14:8A", normalized1)

        val mac2 = "70-85-C2-5B-14-8A"
        val normalized2 = WakeOnLanManager.normalizeMacAddress(mac2)
        assertEquals("70:85:C2:5B:14:8A", normalized2)

        val mac3 = "7085c25b148a"
        val normalized3 = WakeOnLanManager.normalizeMacAddress(mac3)
        assertEquals("70:85:C2:5B:14:8A", normalized3)

        // Invalid MACs
        assertNull(WakeOnLanManager.normalizeMacAddress("invalid-mac"))
        assertNull(WakeOnLanManager.normalizeMacAddress("12:34:56"))
        assertNull(WakeOnLanManager.normalizeMacAddress("GG:HH:II:JJ:KK:LL"))
    }

    @Test
    fun testMacAddressValidation() {
        assertTrue(WakeOnLanManager.isValidMacAddress("70:85:C2:5B:14:8A"))
        assertTrue(WakeOnLanManager.isValidMacAddress("00:11:22:33:44:55"))
        assertTrue(WakeOnLanManager.isValidMacAddress("A1B2C3D4E5F6"))
        assertFalse(WakeOnLanManager.isValidMacAddress(""))
        assertFalse(WakeOnLanManager.isValidMacAddress("00:11:22"))
    }
}
