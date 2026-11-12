package com.ssafy.modi

import androidx.compose.runtime.*

/** Local prototype controller. No commands are sent to a vehicle. */
class AutomationEngine {
    val values = mutableStateMapOf("temperature" to 22, "fan" to 3, "recirculation" to 0,
        "cleaner" to 0, "ac" to 1, "maxAc" to 0, "heat" to 0,
        "leftWindow" to 0, "rightWindow" to 0, "seat" to 1)
    data class Session(val name: String, val priority: Int, val until: Long, val requests: MutableMap<String, Int>)
    val sessions = mutableStateMapOf<String, Session>()
    private val originals = mutableMapOf<String, Int>()
    val events = mutableStateListOf<String>()
    var gear by mutableStateOf("P")
    var speed by mutableStateOf(0)
    var indoor by mutableStateOf(28)
    var outdoor by mutableStateOf(26)
    var rainy by mutableStateOf(false)
    var location by mutableStateOf("판교 · 출발 준비")
    var inOdor by mutableStateOf(false)
    var inTunnel by mutableStateOf(false)
    var polluted by mutableStateOf(false)
    var inDrive by mutableStateOf(false)
    var inParking by mutableStateOf(false)
    var signal by mutableStateOf(true)
    var seatSupported by mutableStateOf(true)
    var windowSupported by mutableStateOf(true)
    var cleanerSupported by mutableStateOf(true)
    var maxAcSupported by mutableStateOf(true)
    var target by mutableStateOf(22)
    var restTemperature by mutableStateOf(24)
    var restFan by mutableStateOf(2)
    var driveFan by mutableStateOf(1)
    var windowPercent by mutableStateOf(50)
    var radius by mutableStateOf(300)
    var airEnabled by mutableStateOf(true)
    var seasonEnabled by mutableStateOf(true)
    var restEnabled by mutableStateOf(true)
    var driveEnabled by mutableStateOf(true)
    var dryEnabled by mutableStateOf(true)
    val safePark get() = gear == "P" && speed == 0
    fun log(message: String) { events.add(0, message); while (events.size > 30) events.removeAt(events.lastIndex) }
    fun start(name: String, priority: Int, duration: Long, request: Map<String, Int>, now: Long = System.currentTimeMillis()) {
        if (sessions.containsKey(name)) return
        request.keys.forEach { originals.putIfAbsent(it, values.getValue(it)) }
        sessions[name] = Session(name, priority, now + duration, request.toMutableMap())
        applyValues()
        log("$name 실행 · 이전 설정 저장")
    }
    private fun applyValues() {
        originals.keys.toList().forEach { field ->
            val owner = sessions.values.filter { it.requests.containsKey(field) }.maxByOrNull { it.priority }
            if (owner == null) { values[field] = originals.getValue(field); originals.remove(field) }
            else values[field] = owner.requests.getValue(field)
        }
    }
    fun finish(name: String) {
        if (name == "휴식" && !safePark && sessions.containsKey(name)) {
            sessions.getValue(name).requests.remove("seat")
            originals.remove("seat")
        }
        if (sessions.remove(name) != null) { applyValues(); log("$name 종료 · 자동 변경 항목 복원") }
    }
    fun manual(field: String, value: Int) {
        sessions.values.forEach { it.requests.remove(field) }
        originals.remove(field)
        values[field] = value
        log("수동 조작 우선 · ${fieldLabel(field)} $value")
    }
    fun startAir() {
        if (!airEnabled) return
        if (inOdor || inTunnel) start("공기 보호", 100, 600_000, mapOf("recirculation" to 1))
        if (cleanerSupported && (inTunnel || polluted))
            start("공기청정", 90, 600_000, mapOf("cleaner" to 1))
    }
    fun exitZone() {
        inOdor = false; inTunnel = false; inDrive = false; inParking = false; signal = true
        finish("공기 보호")
        if (!polluted) finish("공기청정")
        finish("드라이브스루")
        finish("공조 건조")
        location = "판교 · 일반 도로"
        log("구역 이탈 확인")
    }
    fun startRest(): Boolean {
        if (!safePark || !restEnabled) { log("휴식 보류 · P단 및 완전 정차 필요"); return false }
        val request = mutableMapOf("temperature" to restTemperature, "fan" to restFan)
        if (seatSupported) request["seat"] = 2
        start("휴식", 40, 3_600_000, request)
        return true
    }
    fun endRest(): Boolean {
        if (!safePark) { log("휴식 종료 보류 · 안전하게 주차해 주세요"); return false }
        finish("휴식"); return true
    }
    fun startDrive(): Boolean {
        if (!inDrive || speed != 0 || !driveEnabled) return false
        val request = mutableMapOf("fan" to driveFan)
        if (windowSupported) request["leftWindow"] = windowPercent
        start("드라이브스루", 60, 600_000, request); return true
    }
    fun startDry(approvedWindows: Boolean): Boolean {
        if (!inParking || !dryEnabled || values["ac"] != 1) return false
        val request = mutableMapOf("ac" to 0, "fan" to 6)
        if (approvedWindows && !rainy && windowSupported && !safePark) {
            request["leftWindow"] = windowPercent; request["rightWindow"] = windowPercent
        }
        start("공조 건조", 70, 180_000, request); return true
    }
    fun startSeason() {
        if (!seasonEnabled) return
        if (indoor > target + 5) {
            val request = mutableMapOf("temperature" to target, "fan" to 6, "ac" to 1)
            if (maxAcSupported) request["maxAc"] = 1
            start("쾌속 냉방", 30, 300_000, request)
        } else if (indoor < 15 && outdoor < 10) {
            start("초기 난방", 30, 300_000, mapOf("heat" to 3))
        }
    }
    fun tick(now: Long = System.currentTimeMillis()) {
        sessions.values.filter { now >= it.until }.map { it.name }.forEach { finish(it) }
        if (indoor <= target) finish("쾌속 냉방")
        if (indoor >= 20) finish("초기 난방")
        if (speed > 0) finish("드라이브스루")
        if (safePark && sessions.containsKey("공조 건조")) {
            val session = sessions.getValue("공조 건조")
            session.requests.keys.removeAll(setOf("leftWindow", "rightWindow"))
            applyValues()
        }
        // Seat movement is never restored while moving or outside P.
        if (!safePark && sessions.containsKey("휴식")) {
            val seat = values.getValue("seat")
            sessions.getValue("휴식").requests.remove("seat")
            originals.remove("seat")
            values["seat"] = seat
        }
    }
    fun fieldLabel(field: String) = when (field) {
        "temperature" -> "온도"; "fan" -> "풍량"; "recirculation" -> "내기순환"
        "leftWindow" -> "운전석 창문"; "rightWindow" -> "동승석 창문"; else -> field
    }
}
