package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DiscoveredDevice
import com.example.network.DiscoveryManager
import com.example.network.WakeOnLanManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OmniDesk", appName)
    }

    @Test
    fun `send wake on lan magic packet test`() = runBlocking {
        val result = WakeOnLanManager.sendMagicPacket("70:85:C2:5B:14:8A", "127.0.0.1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `wake on lan mac address normalization and validation`() {
        val macWithHyphens = "70-85-C2-5B-14-8A"
        val normalized = WakeOnLanManager.normalizeMacAddress(macWithHyphens)
        assertEquals("70:85:C2:5B:14:8A", normalized)
        assertTrue(WakeOnLanManager.isValidMacAddress("70:85:C2:5B:14:8A"))
        assertFalse(WakeOnLanManager.isValidMacAddress("invalid-mac"))
    }

    @Test
    fun `discovery manager initialization and initial state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = DiscoveryManager(context)
        assertNotNull(manager)
        assertFalse(manager.isScanning.value)
        assertTrue(manager.discoveredDevices.value.isEmpty())
    }

    @Test
    fun `discovered device model properties`() {
        val device = DiscoveredDevice(
            id = "192.168.1.100:8765",
            hostname = "My-Linux-PC",
            ip = "192.168.1.100",
            port = 8765,
            os = "Linux Mint",
            version = "Cinnamon 21.3",
            mac = "70:85:C2:5B:14:8A",
            lastSeen = 1000L
        )
        assertEquals("My-Linux-PC", device.hostname)
        assertEquals("192.168.1.100", device.ip)
        assertEquals(8765, device.port)
        assertEquals("70:85:C2:5B:14:8A", device.mac)
    }

    @Test
    fun `usb cable device representation`() {
        val usbDevice = DiscoveredDevice(
            id = "127.0.0.1:8765",
            hostname = "Linux PC (USB Cable)",
            ip = "127.0.0.1",
            port = 8765,
            os = "Linux (USB Cable)",
            version = "Direct Wired",
            mac = "USB-WIRED",
            lastSeen = System.currentTimeMillis()
        )
        assertEquals("127.0.0.1", usbDevice.ip)
        assertTrue(usbDevice.hostname.contains("USB"))
        assertEquals("USB-WIRED", usbDevice.mac)
    }

    @Test
    fun `preferences manager saves and remembers last connected linux host`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.PreferencesManager(context)

        // Clear initially
        prefs.clearLastConnection()
        assertFalse(prefs.hasSavedConnection())

        // Save successful connection
        val testIp = "192.168.1.150"
        val testPort = 8765
        val testName = "Linux Mint Cinnamon"
        val testToken = "secure_token_abc123"
        val testTime = System.currentTimeMillis()

        prefs.saveLastSuccessfulConnection(
            ip = testIp,
            port = testPort,
            name = testName,
            token = testToken,
            timestamp = testTime
        )

        assertTrue(prefs.hasSavedConnection())
        assertEquals(testIp, prefs.lastConnectedIp)
        assertEquals(testPort, prefs.lastConnectedPort)
        assertEquals(testName, prefs.lastConnectedName)
        assertEquals(testToken, prefs.lastConnectedToken)
        assertEquals(testTime, prefs.lastConnectedTimestamp)
        assertFalse(prefs.isUsbMode)
        assertEquals("Just now", prefs.getFormattedLastConnectedTime())

        // Test clear
        prefs.clearLastConnection()
        assertFalse(prefs.hasSavedConnection())
        assertEquals(null, prefs.lastConnectedIp)
        assertEquals(0L, prefs.lastConnectedTimestamp)
    }

    @Test
    fun `preferences manager auto-reconnect toggle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.PreferencesManager(context)

        // Default should be true
        assertTrue(prefs.autoReconnectEnabled)

        prefs.autoReconnectEnabled = false
        assertFalse(prefs.autoReconnectEnabled)

        prefs.autoReconnectEnabled = true
        assertTrue(prefs.autoReconnectEnabled)
    }

    @Test
    fun `preferences manager usb host detection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.PreferencesManager(context)

        prefs.saveLastSuccessfulConnection(
            ip = "127.0.0.1",
            port = 8765,
            name = "Linux PC (USB Cable)",
            token = "usb_token"
        )

        assertTrue(prefs.isUsbMode)
        assertEquals("127.0.0.1", prefs.lastConnectedIp)

        prefs.clearLastConnection()
    }

    @Test
    fun `preferences manager single tap action toggle and default`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.PreferencesManager(context)

        // Default should be "left"
        assertEquals("left", prefs.singleTapAction)
        assertTrue(prefs.tapToClickEnabled)

        // Set to "right"
        prefs.singleTapAction = "right"
        assertEquals("right", prefs.singleTapAction)

        // Set to "left"
        prefs.singleTapAction = "left"
        assertEquals("left", prefs.singleTapAction)

        // Toggle tapToClickEnabled
        prefs.tapToClickEnabled = false
        assertFalse(prefs.tapToClickEnabled)
        prefs.tapToClickEnabled = true
        assertTrue(prefs.tapToClickEnabled)
    }

    @Test
    fun `database creation and paired device dao with osType`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getDatabase(context)
        val dao = db.pairedDeviceDao()
        val device = com.example.data.entity.PairedDeviceEntity(
            name = "Workstation",
            ip = "192.168.1.80",
            port = 8765,
            token = "token123",
            macOrUid = "00:11:22:33:44:55",
            osName = "Windows 11",
            osType = "windows"
        )
        dao.insertDevice(device)
        val loaded = dao.getDeviceByIp("192.168.1.80")
        assertNotNull(loaded)
        assertEquals("windows", loaded?.osType)
        assertTrue(loaded?.isWindows == true)
        assertFalse(loaded?.isMac == true)
    }

    @Test
    fun `qr pairing payload parses json format correctly`() {
        val json = """{"protocol":"omnidesk","ip":"192.168.1.45","port":8765,"pin":"982143","hostname":"My-MacBook","os":"macOS"}"""
        val payload = com.example.model.QrPairingPayload.parse(json)
        assertNotNull(payload)
        assertEquals("192.168.1.45", payload?.ip)
        assertEquals(8765, payload?.port)
        assertEquals("982143", payload?.pin)
        assertEquals("My-MacBook", payload?.hostname)
        assertEquals("macOS", payload?.os)
    }

    @Test
    fun `qr pairing payload parses uri format correctly`() {
        val uri = "omnidesk://192.168.1.99:8765?pin=456789&name=Gaming-Rig&os=windows"
        val payload = com.example.model.QrPairingPayload.parse(uri)
        assertNotNull(payload)
        assertEquals("192.168.1.99", payload?.ip)
        assertEquals(8765, payload?.port)
        assertEquals("456789", payload?.pin)
        assertEquals("Gaming-Rig", payload?.hostname)
        assertEquals("windows", payload?.os)
    }

    @Test
    fun `qr pairing payload parses simple delimited format correctly`() {
        val simple = "192.168.1.200:8765:654321"
        val payload = com.example.model.QrPairingPayload.parse(simple)
        assertNotNull(payload)
        assertEquals("192.168.1.200", payload?.ip)
        assertEquals(8765, payload?.port)
        assertEquals("654321", payload?.pin)
    }

    @Test
    fun `qr pairing payload returns null for invalid input`() {
        val invalid = "just random text without ip or json"
        val payload = com.example.model.QrPairingPayload.parse(invalid)
        assertEquals(null, payload)
    }

    @Test
    fun `repository saveDevice updates existing device without duplicating`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getDatabase(context)
        val dao = db.pairedDeviceDao()
        val repo = com.example.data.repository.DeviceRepository(dao)

        val dev1 = com.example.data.entity.PairedDeviceEntity(
            name = "TestPC",
            ip = "192.168.1.10",
            port = 8765,
            token = "old_token",
            osName = "Windows"
        )
        repo.saveDevice(dev1)

        val dev2 = com.example.data.entity.PairedDeviceEntity(
            name = "TestPC Updated",
            ip = "192.168.1.10",
            port = 8765,
            token = "new_token",
            osName = "Windows 11"
        )
        repo.saveDevice(dev2)

        val loaded = repo.getDeviceByIp("192.168.1.10")
        assertNotNull(loaded)
        assertEquals("new_token", loaded?.token)
        assertEquals("TestPC Updated", loaded?.name)
    }

    @Test
    fun `preferences manager clearSavedToken clears token only`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.PreferencesManager(context)

        prefs.saveLastSuccessfulConnection(
            ip = "192.168.1.77",
            port = 8765,
            name = "MyPC",
            token = "token_xyz"
        )
        assertEquals("token_xyz", prefs.lastConnectedToken)
        assertEquals("192.168.1.77", prefs.lastConnectedIp)

        prefs.clearSavedToken("192.168.1.77")
        assertEquals(null, prefs.lastConnectedToken)
        assertEquals("192.168.1.77", prefs.lastConnectedIp)
    }
}
