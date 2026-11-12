package com.ssafy.modi

import org.junit.Assert.*
import org.junit.Test

class AutomationEngineTest {
    @Test fun manualChangeSurvivesExitAndOverlappingModes() {
        val e = AutomationEngine()
        e.inOdor = true; e.startAir()
        e.inDrive = true; e.startDrive()
        assertEquals(1, e.values["recirculation"])
        assertEquals(1, e.values["fan"])
        e.manual("fan", 4)
        e.exitZone()
        assertEquals(4, e.values["fan"])
        assertEquals(0, e.values["recirculation"])
        assertEquals(0, e.values["leftWindow"])
    }
    @Test fun lastAirCleanerConditionRestoresOriginal() {
        val e = AutomationEngine()
        e.inTunnel = true; e.polluted = true; e.startAir()
        e.exitZone()
        assertEquals(1, e.values["cleaner"])
        e.polluted = false; e.finish("공기청정")
        assertEquals(0, e.values["cleaner"])
    }
    @Test fun restRequiresParkAndStationary() {
        val e = AutomationEngine()
        e.gear = "D"; assertFalse(e.startRest())
        e.gear = "P"; e.speed = 1; assertFalse(e.startRest())
        e.speed = 0; assertTrue(e.startRest())
        assertEquals(2, e.values["seat"])
        e.gear = "D"; assertFalse(e.endRest())
        e.tick(Long.MAX_VALUE)
        assertEquals(2, e.values["seat"])
    }
    @Test fun rainNeverOpensWindowsAndParkRestoresDryVentilation() {
        val e = AutomationEngine()
        e.inParking = true; e.gear = "D"; e.speed = 5; e.rainy = true
        assertTrue(e.startDry(true))
        assertEquals(0, e.values["leftWindow"])
        e.finish("공조 건조"); e.rainy = false
        e.startDry(true)
        assertEquals(50, e.values["leftWindow"])
        e.gear = "P"; e.speed = 0; e.tick()
        assertEquals(0, e.values["leftWindow"])
        assertEquals(6, e.values["fan"])
    }
    @Test fun priorityStackReturnsToUnderlyingModeThenOriginal() {
        val e = AutomationEngine()
        e.start("low", 1, 1000, mapOf("fan" to 2), 0)
        e.start("high", 2, 100, mapOf("fan" to 6), 0)
        e.tick(101)
        assertEquals(2, e.values["fan"])
        e.tick(1001)
        assertEquals(3, e.values["fan"])
    }
    @Test fun coolingStopsAtTargetAndUnsupportedMaxAcUsesFan() {
        val e = AutomationEngine()
        e.maxAcSupported = false; e.indoor = 38; e.startSeason()
        assertEquals(0, e.values["maxAc"])
        assertEquals(6, e.values["fan"])
        e.manual("temperature", 25)
        e.indoor = 22; e.tick()
        assertEquals(3, e.values["fan"])
        assertEquals(25, e.values["temperature"])
    }
    @Test fun timeoutRestoresAirEvenWithoutLocationSignal() {
        val e = AutomationEngine()
        e.inTunnel = true; e.startAir(); e.signal = false
        e.tick(Long.MAX_VALUE)
        assertEquals(0, e.values["recirculation"])
        assertEquals(0, e.values["cleaner"])
        assertTrue(e.sessions.isEmpty())
    }
}
