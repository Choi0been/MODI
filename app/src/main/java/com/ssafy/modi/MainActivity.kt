package com.ssafy.modi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@androidx.compose.ui.tooling.preview.Preview(name = "MODI Automotive", widthDp = 1400, heightDp = 900)
@Composable
private fun AutomotivePreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Paper, surface = Color.White, onSurface = Ink)) {
        ModiApp(remember { DashboardModel() })
    }
}

private val Ink = Color(0xFF202B28)
private val Muted = Color(0xFF7B8580)
private val Green = Color(0xFF36785B)
private val Lime = Color(0xFFD5F49C)
private val Paper = Color(0xFFF4F6F2)
private val Line = Color(0xFFE6EAE4)
private val Shape = RoundedCornerShape(22.dp)

class DashboardModel : ViewModel() {
    val engine = AutomationEngine()
    var initialized = false
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this)[DashboardModel::class.java]
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Paper, surface = Color.White, onSurface = Ink)) { ModiApp(model) } }
    }
}

@Composable
private fun ModiApp(model: DashboardModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("modi_preferences", 0) }
    val engine = remember(model) { model.engine.apply {
        if (!model.initialized) {
        target = prefs.getInt("target", 22); restTemperature = prefs.getInt("restTemp", 24)
        restFan = prefs.getInt("restFan", 2); radius = prefs.getInt("radius", 300)
        windowPercent = prefs.getInt("window", 50); driveFan = prefs.getInt("driveFan", 1)
        airEnabled = prefs.getBoolean("air", true); seasonEnabled = prefs.getBoolean("season", true)
        restEnabled = prefs.getBoolean("rest", true); driveEnabled = prefs.getBoolean("drive", true)
        dryEnabled = prefs.getBoolean("dry", true)
        log("MODI 준비 완료 · 시뮬레이션 차량 연결")
        model.initialized = true
        }
    } }
    var revision by remember { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var dialog by rememberSaveable { mutableStateOf("") }
    val auto = engine.airEnabled && engine.restEnabled && engine.seasonEnabled && engine.driveEnabled && engine.dryEnabled
    val zones = remember { mutableStateListOf<String>().apply {
        val saved = JSONArray(prefs.getString("zones", "[]"))
        for (i in 0 until saved.length()) add(saved.getString(i))
    } }
    fun update() {
        revision++
        prefs.edit().putInt("target", engine.target).putInt("restTemp", engine.restTemperature)
            .putInt("restFan", engine.restFan).putInt("radius", engine.radius)
            .putInt("window", engine.windowPercent).putInt("driveFan", engine.driveFan)
            .putBoolean("air", engine.airEnabled).putBoolean("season", engine.seasonEnabled)
            .putBoolean("rest", engine.restEnabled).putBoolean("drive", engine.driveEnabled)
            .putBoolean("dry", engine.dryEnabled).putString("zones", JSONArray(zones.toList()).toString()).apply()
    }
    LaunchedEffect(engine) { while (true) { delay(1000); engine.tick(); revision++ } }
    // Session state is kept above navigation; preferences are persisted separately from the demo vehicle.
    @Suppress("UNUSED_VARIABLE") val repaint = revision
    BoxWithConstraints(Modifier.fillMaxSize().background(Paper).safeDrawingPadding()) {
        val wide = maxWidth >= 840.dp
        val extraWide = maxWidth > 1150.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) Sidebar(page, { page = it }, { dialog = "about" })
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = if (wide) 30.dp else 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("PLEOS CONNECT  /  MODI", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(listOf("마이 드라이브", "나의 구역", "자동화 설정", "활동 기록")[page], fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                    Pill("●  시뮬레이션", Color(0xFFE6EDDF), Green) { dialog = "sim" }
                    Spacer(Modifier.width(18.dp))
                    if (wide) { Text(SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()), color = Ink, fontSize = 18.sp); Spacer(Modifier.width(18.dp)) }
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Ink).clickable { dialog = "about" }, contentAlignment = Alignment.Center) {
                        Text("M", color = Lime, fontWeight = FontWeight.Bold)
                    }
                }
                if (!wide) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("대시보드", "나의 구역", "자동화 설정", "활동 기록").forEachIndexed { index, title ->
                        FilterChip(page == index, { page = index }, label = { Text(title) })
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 30.dp else 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    when (page) {
                        0 -> {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("좋은 드라이브의 시작", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                    Text("반복되는 조작은 MODI에게 맡기세요.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                                TextButton(onClick = { dialog = "sim" }) { Text("시나리오 체험  ↗", fontWeight = FontWeight.Bold) }
                            }
                            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                Hero(engine, auto, { dialog = "status" }, Modifier.weight(1.65f))
                                Environment(engine, Modifier.weight(1f)) { dialog = "climate" }
                            } else {
                                Hero(engine, auto, { dialog = "status" }, Modifier.fillMaxWidth())
                                Environment(engine, Modifier.fillMaxWidth()) { dialog = "climate" }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("나를 위한 자동화", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                                Text("전체 자동화", fontSize = 12.sp, color = Muted)
                                Spacer(Modifier.width(10.dp))
                                Switch(auto, { enabled ->
                                    engine.airEnabled = enabled; engine.restEnabled = enabled; engine.seasonEnabled = enabled
                                    engine.driveEnabled = enabled; engine.dryEnabled = enabled
                                    if (!enabled) engine.sessions.keys.toList().forEach { if (it != "휴식" || engine.safePark) engine.finish(it) }
                                    update()
                                }, colors = SwitchDefaults.colors(checkedTrackColor = Green))
                            }
                            val cards = listOf(
                                Mode("air", "공기 보호", "악취는 밖에, 쾌적함은 안에", "악취 구역 · 터널 · 미세먼지", engine.airEnabled, "wind"),
                                Mode("rest", "휴식 모드", "잠깐의 쉼도 편안하게", "공조 · 시트 메모리", engine.restEnabled, "moon"),
                                Mode("season", "쾌적한 탑승", "타는 순간, 알맞은 온도로", "여름 냉방 · 겨울 열선", engine.seasonEnabled, "sun"),
                                Mode("drive", "드라이브스루", "주문과 결제에만 집중", "창문 개방 · 저소음 송풍", engine.driveEnabled, "cup"),
                                Mode("dry", "공조 건조", "드라이브의 끝까지 산뜻하게", "주차장 진입 · 송풍 건조", engine.dryEnabled, "drop"))
                            val cols = if (wide && extraWide) 5 else if (wide) 3 else 2
                            cards.chunked(cols).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { mode -> ModeCard(mode, Modifier.weight(1f)) { dialog = mode.id } }
                                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            Row(Modifier.fillMaxWidth().clip(Shape).background(Color(0xFFE9EEE4)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Glyph("shield", Green, 28)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("운전자의 선택이 언제나 먼저", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("직접 바꾼 설정은 유지하고, 자동으로 바꾼 항목만 복원해요.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                                TextButton(onClick = { page = 3 }) { Text("활동 보기  →") }
                            }
                        }
                        1 -> ZonesPage(engine, zones, { dialog = "addZone" }, { update() })
                        2 -> SettingsPage(engine, { update() }, { dialog = it })
                        3 -> {
                            SectionTitle("MODI가 챙긴 순간들", "현재 시뮬레이션 세션의 제어 및 복원 기록입니다.")
                            if (engine.events.isEmpty()) Text("아직 활동이 없어요.", color = Muted)
                            engine.events.forEachIndexed { index, event ->
                                Row(Modifier.fillMaxWidth().clip(Shape).background(Color.White).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(if (index == 0) Lime else Paper), contentAlignment = Alignment.Center) { Glyph("check", Green, 18) }
                                    Text(event, Modifier.weight(1f).padding(start = 16.dp), color = Ink, fontSize = 14.sp)
                                    Text(if (index == 0) "최근" else "이전", color = Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MODI  ·  Make your drive yours.", fontSize = 11.sp, color = Muted)
                        Text("예시 차량 · 외부 데이터 미연결", fontSize = 11.sp, color = Muted)
                    }
                }
            }
        }
    }
    if (dialog.isNotEmpty()) {
        val dismiss = { dialog = ""; update() }
        when (dialog) {
            "sim" -> AppDialog("드라이브 시뮬레이터", "상황을 선택해 자동 제어와 복원을 확인하세요.", dismiss) {
                listOf("악취 구역 진입" to "내기순환 자동 전환", "터널 진입" to "내기순환 + 공기청정", "휴게소 도착" to "P단 · 정차 · 휴식 제안",
                    "여름철 탑승" to "실내 38°C · 쾌속 냉방", "겨울철 탑승" to "실내 5°C · 초기 열선", "드라이브스루 도착" to "정차 · 창문 개방 제안",
                    "집 주차장 진입" to "송풍 건조 · 환기 승인", "구역 이탈" to "이전 설정으로 복원").forEachIndexed { i, (title, sub) ->
                    ActionRow(title, sub) {
                        when (i) {
                            0 -> { engine.inOdor = true; engine.location = "분당 하수처리장 인근"; engine.startAir(); dialog = "" }
                            1 -> { engine.inTunnel = true; engine.location = "판교 터널 · 진입"; engine.startAir(); dialog = "" }
                            2 -> { engine.gear = "P"; engine.speed = 0; engine.location = "서울만남의광장 휴게소"; dialog = "rest" }
                            3 -> { engine.indoor = 38; engine.outdoor = 33; engine.startSeason(); dialog = "" }
                            4 -> { engine.indoor = 5; engine.outdoor = -3; engine.startSeason(); dialog = "" }
                            5 -> { engine.inDrive = true; engine.speed = 0; engine.gear = "D"; engine.location = "드라이브스루 · 주문 지점"; dialog = "drive" }
                            6 -> { engine.inParking = true; engine.speed = 5; engine.gear = "D"; engine.location = "우리 집 · 주차장 진입"; dialog = "dry" }
                            7 -> { engine.exitZone(); dialog = "" }
                        }; update()
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("차량 상태", Modifier.weight(1f), fontSize = 13.sp)
                    TextButton(onClick = { engine.gear = "P"; engine.speed = 0; engine.tick(); update() }) { Text("P · 정차") }
                    TextButton(onClick = { engine.gear = "D"; engine.speed = 30; engine.tick(); update() }) { Text("D · 30 km/h") }
                }
                ToggleRow("우천 상황", engine.rainy) { engine.rainy = it; update() }
                ToggleRow("위치 신호", engine.signal) { engine.signal = it; engine.log(if (it) "위치 신호 복구" else "위치 신호 유실 · 제한 시간까지 보호 유지"); update() }
                ToggleRow("미세먼지 기준 초과", engine.polluted) {
                    engine.polluted = it
                    if (it) engine.startAir() else if (!engine.inTunnel) engine.finish("공기청정")
                    update()
                }
                TextButton(onClick = { engine.indoor = engine.target; engine.tick(); update() }) { Text("목표 온도 도달 재현") }
            }
            "air", "rest", "season", "drive", "dry" -> {
                val id = dialog
                val title = mapOf("air" to "공기 보호", "rest" to "휴식 모드", "season" to "쾌적한 탑승", "drive" to "드라이브스루", "dry" to "공조 건조").getValue(id)
                AppDialog(title, "차량 상태와 사용자의 선호에 맞춰 작동합니다.", dismiss) {
                    when (id) {
                        "air" -> {
                            ToggleRow("자동 공기 보호", engine.airEnabled) { engine.airEnabled = it; if (!it) { engine.finish("공기 보호"); engine.finish("공기청정") }; update() }
                            Stepper("감지 반경", engine.radius, "m", { engine.radius = (engine.radius - 100).coerceAtLeast(100); update() }, { engine.radius = (engine.radius + 100).coerceAtMost(1000); update() })
                            Note("최대 10분 유지 후 복원합니다. 터널에서 위치가 끊기면 출구 재확인 또는 제한 시간까지 유지합니다.")
                            Primary("악취 구역 체험") { engine.inOdor = true; engine.startAir(); dismiss() }
                        }
                        "rest" -> {
                            Note("P단 · 속도 0 km/h에서 승인한 경우에만 시작합니다. 시트 메모리 미지원 차량은 공조만 적용합니다.")
                            Stepper("휴식 온도", engine.restTemperature, "°C", { engine.restTemperature = (engine.restTemperature - 1).coerceAtLeast(18); update() }, { engine.restTemperature = (engine.restTemperature + 1).coerceAtMost(28); update() })
                            Stepper("휴식 풍량", engine.restFan, "단", { engine.restFan = (engine.restFan - 1).coerceAtLeast(1); update() }, { engine.restFan = (engine.restFan + 1).coerceAtMost(6); update() })
                            Text("현재 상태  ${engine.gear}단 · ${engine.speed} km/h", color = Green)
                            Primary(if (engine.sessions.containsKey("휴식")) "휴식 종료 · 이전 설정 복원" else "동의하고 휴식 시작", engine.safePark) {
                                if (engine.sessions.containsKey("휴식")) engine.endRest() else engine.startRest(); dismiss()
                            }
                        }
                        "season" -> {
                            ToggleRow("계절별 자동 보조", engine.seasonEnabled) { engine.seasonEnabled = it; if (!it) { engine.finish("쾌속 냉방"); engine.finish("초기 난방") }; update() }
                            Stepper("목표 온도", engine.target, "°C", { engine.target = (engine.target - 1).coerceAtLeast(18); update() }, { engine.target = (engine.target + 1).coerceAtMost(28); update() })
                            Note("여름에는 목표보다 5°C 초과 시 냉방을 시작합니다. 겨울에는 실내 15°C 미만·외기 10°C 미만에서 열선을 사용합니다. 최대 5분 후 복원합니다.")
                            Primary("현재 온도로 실행") { engine.startSeason(); dismiss() }
                        }
                        "drive" -> {
                            Note("등록 구역 내 완전 정차 시에만 시작합니다. 움직이거나 구역을 벗어나면 복원합니다. 악취 구역에서는 내기순환을 유지합니다.")
                            Stepper("창문 개방", engine.windowPercent, "%", { engine.windowPercent = (engine.windowPercent - 10).coerceAtLeast(10); update() }, { engine.windowPercent = (engine.windowPercent + 10).coerceAtMost(100); update() })
                            Stepper("대화 중 풍량", engine.driveFan, "단", { engine.driveFan = (engine.driveFan - 1).coerceAtLeast(0); update() }, { engine.driveFan = (engine.driveFan + 1).coerceAtMost(3); update() })
                            Primary(if (engine.sessions.containsKey("드라이브스루")) "모드 종료 · 복원" else "동의하고 창문 개방", engine.sessions.containsKey("드라이브스루") || (engine.inDrive && engine.speed == 0 && engine.driveEnabled)) {
                                if (engine.sessions.containsKey("드라이브스루")) engine.finish("드라이브스루") else engine.startDrive(); dismiss()
                            }
                            if (!engine.inDrive) TextButton(onClick = { dialog = "sim" }) { Text("시뮬레이터에서 구역 진입하기") }
                        }
                        "dry" -> {
                            Note(if (engine.rainy) "우천 상황입니다. 창문은 조작하지 않고 송풍만 실행합니다." else "창문을 열어도 될까요? 승인하면 1열 창문을 설정한 만큼 개방합니다. P단·정차 시 원래 위치로 복원합니다.")
                            Text("A/C 끄기 · 송풍 6단 · 최대 3분", color = Green, fontSize = 14.sp)
                            Primary(if (engine.rainy) "송풍 건조 시작" else "동의하고 건조 · 환기 시작", engine.inParking && engine.dryEnabled && engine.values["ac"] == 1) { engine.startDry(true); dismiss() }
                            TextButton(onClick = { engine.startDry(false); dismiss() }, enabled = engine.inParking && engine.values["ac"] == 1) { Text("창문 없이 송풍만 시작") }
                            if (engine.sessions.containsKey("공조 건조")) TextButton(onClick = { engine.finish("공조 건조"); dismiss() }) { Text("건조 종료 · 복원") }
                            if (!engine.inParking) TextButton(onClick = { dialog = "sim" }) { Text("주차장 진입 상황 체험") }
                        }
                    }
                }
            }
            "climate" -> AppDialog("실내 환경 직접 조작", "수동으로 바꾼 항목은 자동 복원에서 제외됩니다.", dismiss) {
                Stepper("설정 온도", engine.values.getValue("temperature"), "°C", { engine.manual("temperature", (engine.values.getValue("temperature") - 1).coerceAtLeast(16)); update() }, { engine.manual("temperature", (engine.values.getValue("temperature") + 1).coerceAtMost(30)); update() })
                Stepper("풍량", engine.values.getValue("fan"), "단", { engine.manual("fan", (engine.values.getValue("fan") - 1).coerceAtLeast(0)); update() }, { engine.manual("fan", (engine.values.getValue("fan") + 1).coerceAtMost(6)); update() })
                ToggleRow("내기순환", engine.values["recirculation"] == 1) { engine.manual("recirculation", if (it) 1 else 0); update() }
                ToggleRow("A/C", engine.values["ac"] == 1) { engine.manual("ac", if (it) 1 else 0); update() }
                Stepper("운전석 창문", engine.values.getValue("leftWindow"), "%", { engine.manual("leftWindow", (engine.values.getValue("leftWindow") - 10).coerceAtLeast(0)); update() }, { engine.manual("leftWindow", (engine.values.getValue("leftWindow") + 10).coerceAtMost(100)); update() })
            }
            "status" -> AppDialog("실행 중인 자동화", "변경한 항목과 남은 시간을 확인하세요.", dismiss) {
                if (engine.sessions.isEmpty()) Note("실행 중인 자동화가 없습니다. 조건을 감지하면 MODI가 알려드려요.")
                engine.sessions.values.toList().forEach { session ->
                    ActionRow(session.name, "${((session.until - System.currentTimeMillis()) / 1000).coerceAtLeast(0)}초 남음 · ${session.requests.keys.joinToString { engine.fieldLabel(it) }}") {
                        if (session.name == "휴식") engine.endRest() else engine.finish(session.name); update()
                    }
                }
                Note("항목을 선택하면 해당 모드를 종료합니다. 휴식 종료는 P단·정차 시 가능합니다.")
            }
            "addZone" -> {
                var name by rememberSaveable { mutableStateOf("") }
                AppDialog("나의 구역 추가", "프로토타입의 지도는 예시입니다. 장소 이름을 저장해 시나리오를 연결할 수 있어요.", dismiss) {
                    OutlinedTextField(name, { name = it }, label = { Text("장소 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Primary("악취 구역 저장", name.isNotBlank()) { zones.add(name.trim()); engine.log("사용자 구역 추가 · ${name.trim()}"); dismiss() }
                }
            }
            else -> AppDialog("MODI, 나에게 맞춰지는 드라이브", "Pleos Connect 차량용 앱 프로토타입", dismiss) {
                Note("차량 상태·위치·날씨·대기질은 시뮬레이션 데이터입니다. 현재 앱은 실제 차량 장치를 제어하지 않습니다.")
                Note("실차 연동 시 Vehicle SDK의 지원 범위 및 권한 확인, FusedLocation 구독, AAOS 기어·속도 구독과 실패 후 재조회가 필요합니다.")
                ToggleRow("시트 메모리 지원", engine.seatSupported) { engine.seatSupported = it; update() }
                ToggleRow("창문 제어 지원", engine.windowSupported) { engine.windowSupported = it; update() }
                ToggleRow("공기청정 지원", engine.cleanerSupported) { engine.cleanerSupported = it; update() }
                ToggleRow("Max A/C 지원", engine.maxAcSupported) { engine.maxAcSupported = it; update() }
            }
        }
    }
}

private data class Mode(val id: String, val title: String, val sub: String, val detail: String, val enabled: Boolean, val icon: String)

@Composable private fun Sidebar(page: Int, select: (Int) -> Unit, about: () -> Unit) {
    Column(Modifier.width(104.dp).fillMaxHeight().background(Ink).padding(vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("modi", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Lime, letterSpacing = (-2).sp)
        Text("MY DRIVE", fontSize = 7.sp, letterSpacing = 2.sp, color = Color(0xFF9DA99F), modifier = Modifier.padding(top = 5.dp))
        Spacer(Modifier.height(44.dp))
        listOf("대시보드" to "grid", "나의 구역" to "pin", "자동화 설정" to "sliders", "활동 기록" to "clock").forEachIndexed { index, (title, icon) ->
            Column(Modifier.padding(horizontal = 10.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (page == index) Lime else Color.Transparent).clickable { select(index) }.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Glyph(icon, if (page == index) Ink else Color(0xFFA6B1A9), 23)
                Text(title, fontSize = 10.sp, color = if (page == index) Ink else Color(0xFFA6B1A9), modifier = Modifier.padding(top = 9.dp), fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = about), contentAlignment = Alignment.Center) { Glyph("settings", Color(0xFFAFB9B2), 24) }
        Text("v0.1 DEMO", color = Color(0xFF7F8C83), fontSize = 8.sp, modifier = Modifier.padding(top = 15.dp))
    }
}

@Composable private fun Hero(e: AutomationEngine, auto: Boolean, open: () -> Unit, modifier: Modifier) {
    Box(modifier.height(278.dp).clip(Shape).background(Brush.linearGradient(listOf(Color(0xFF273B32), Color(0xFF172821)))).clickable(onClick = open)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFF354C3D).copy(alpha = .4f), size.width * .43f, Offset(size.width * .85f, size.height * .7f), style = Stroke(1.dp.toPx()))
            drawCircle(Color(0xFF354C3D).copy(alpha = .4f), size.width * .33f, Offset(size.width * .85f, size.height * .7f), style = Stroke(1.dp.toPx()))
        }
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Lime, CircleShape)); Spacer(Modifier.width(7.dp))
                Text(if (auto) "MODI READY" else "MODI PAUSED", color = Lime, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (e.sessions.isEmpty()) "당신은 운전에만\n집중하세요." else "지금, MODI가\n쾌적함을 챙기고 있어요.", color = Color.White, fontSize = 27.sp, lineHeight = 37.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp))
            Text(if (e.sessions.isEmpty()) "나머지는 MODI가 알아서." else e.sessions.keys.joinToString(" · "), color = Color(0xFFA5B9AC), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
        CarIllustration(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 35.dp).fillMaxWidth(.57f).height(155.dp))
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = .12f)).padding(horizontal = 24.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph("pin", Lime, 14); Spacer(Modifier.width(7.dp))
            Text(e.location, color = Color(0xFFD8E4DA), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${e.gear}  ·  ${e.speed} km/h", color = Lime, fontSize = 11.sp)
        }
    }
}

@Composable private fun Environment(e: AutomationEngine, modifier: Modifier, open: () -> Unit) {
    Column(modifier.height(278.dp).clip(Shape).background(Color.White).padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("지금, 실내는", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Pill("쾌적", Color(0xFFEDF5E8), Green)
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text("${e.values["temperature"]}", color = Ink, fontSize = 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp)
            Text("°C", color = Muted, fontSize = 24.sp, modifier = Modifier.padding(bottom = 15.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("실내 ${e.indoor}°", color = Ink, fontSize = 13.sp)
                Text("외부 ${e.outdoor}°", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("풍량", "${e.values["fan"]}단", "wind")
            Metric("흡기", if (e.values["recirculation"] == 1) "내기순환" else "외기유입", "cycle")
            Metric("PM2.5", if (e.polluted) "나쁨" else "좋음", "leaf")
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Paper).clickable(onClick = open).padding(10.dp), horizontalArrangement = Arrangement.Center) {
            Text("실내 환경 조절", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium); Text("  ↗", color = Green)
        }
    }
}

@Composable private fun Metric(label: String, value: String, icon: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) { Glyph(icon, Muted, 13); Text(" $label", color = Muted, fontSize = 10.sp) }
        Text(value, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable private fun ModeCard(mode: Mode, modifier: Modifier, open: () -> Unit) {
    Column(modifier.height(194.dp).clip(Shape).background(Color.White).clickable(onClick = open).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF0F4E9)), contentAlignment = Alignment.Center) { Glyph(mode.icon, Green, 24) }
            Spacer(Modifier.weight(1f))
            Text(if (mode.enabled) "ON" else "OFF", color = if (mode.enabled) Green else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(mode.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(mode.sub, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mode.detail, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f))
            Text("↗", color = Green, fontSize = 16.sp)
        }
    }
}

@Composable private fun ZonesPage(e: AutomationEngine, zones: MutableList<String>, add: () -> Unit, changed: () -> Unit) {
    SectionTitle("매일 지나가는 길도, 더 쾌적하게", "기본 구역과 직접 등록한 장소를 함께 관리하세요.")
    Box(Modifier.fillMaxWidth().height(280.dp).clip(Shape).background(Color(0xFFE7EDDF))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            for (i in 0..9) {
                drawLine(Color(0xFFD7E1D1), Offset(w * i / 9, 0f), Offset(w * i / 9 + 80, h), 1.dp.toPx())
                drawLine(Color.White, Offset(0f, h * i / 9), Offset(w, h * i / 9 - 60), 8.dp.toPx())
            }
            val road = Path().apply { moveTo(0f, h * .8f); cubicTo(w * .3f, h * .1f, w * .5f, h * 1.2f, w, h * .3f) }
            drawPath(road, Color.White, style = Stroke(26.dp.toPx()))
            drawPath(road, Color(0xFFB5C9A7), style = Stroke(2.dp.toPx()))
            drawCircle(Green.copy(alpha = .12f), 65.dp.toPx(), Offset(w * .6f, h * .5f))
            drawCircle(Green.copy(alpha = .4f), 65.dp.toPx(), Offset(w * .6f, h * .5f), style = Stroke(1.dp.toPx()))
            drawCircle(Green, 8.dp.toPx(), Offset(w * .6f, h * .5f))
        }
        Pill("판교 주변 · 예시 지도", Color.White, Ink, Modifier.align(Alignment.TopStart).padding(18.dp))
        Column(Modifier.align(Alignment.Center).offset(x = 80.dp, y = (-18).dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp)) {
            Text("분당 하수처리장", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("보호 반경 ${e.radius} m", color = Green, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text("실제 위치 서비스 미연결", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) { Text("등록된 구역  ${3 + zones.size}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Button(onClick = add) { Text("+ 구역 추가") } }
    listOf("분당 하수처리장" to "악취 보호 · 기본 구역", "판교 터널" to "터널 · 내기순환 및 공기청정", "우리 집 주차장" to "주차장 · 송풍 건조 제안").forEach { (name, sub) ->
        Row(Modifier.fillMaxWidth().clip(Shape).background(Color.White).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph("pin", Green, 24)
            Column(Modifier.weight(1f).padding(start = 16.dp)) { Text(name, color = Ink, fontWeight = FontWeight.Bold); Text(sub, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
            Text("기본", color = Green, fontSize = 11.sp)
        }
    }
    zones.toList().forEach { name -> Row(Modifier.fillMaxWidth().clip(Shape).background(Color.White).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Glyph("pin", Green, 24); Text(name, color = Ink, modifier = Modifier.weight(1f).padding(start = 16.dp))
        TextButton(onClick = { zones.remove(name); changed() }) { Text("삭제") }
    } }
}

@Composable private fun SettingsPage(e: AutomationEngine, changed: () -> Unit, open: (String) -> Unit) {
    SectionTitle("당신의 취향대로, MODI답게", "설정은 차량 내 앱에 저장됩니다.")
    Column(Modifier.clip(Shape).background(Color.White).padding(22.dp)) {
        ToggleRow("악취 구역 · 공기 보호", e.airEnabled) { e.airEnabled = it; if (!it) { e.finish("공기 보호"); e.finish("공기청정") }; changed() }
        ToggleRow("정차 중 휴식 제안", e.restEnabled) { e.restEnabled = it; changed() }
        ToggleRow("탑승 시 냉난방 보조", e.seasonEnabled) { e.seasonEnabled = it; if (!it) { e.finish("쾌속 냉방"); e.finish("초기 난방") }; changed() }
        ToggleRow("드라이브스루 제안", e.driveEnabled) { e.driveEnabled = it; changed() }
        ToggleRow("주차장 공조 건조 제안", e.dryEnabled) { e.dryEnabled = it; changed() }
    }
    ActionRow("공기 보호 조건", "감지 반경 ${e.radius} m · 최대 10분") { open("air") }
    ActionRow("휴식 프로필", "${e.restTemperature}°C · 풍량 ${e.restFan}단 · 휴식 시트 메모리 2") { open("rest") }
    ActionRow("탑승 시 목표 온도", "${e.target}°C · 최대 5분") { open("season") }
    ActionRow("창문 및 대화 설정", "개방 ${e.windowPercent}% · 풍량 ${e.driveFan}단") { open("drive") }
    ActionRow("차량 지원 기능", "시뮬레이션 차량의 하드웨어 지원 여부 변경") { open("about") }
}

@Composable private fun SectionTitle(title: String, sub: String) {
    Column { Text(title, fontSize = 23.sp, color = Ink, fontWeight = FontWeight.Bold); Text(sub, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
}
@Composable private fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, action: (() -> Unit)? = null) {
    Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = modifier.clip(RoundedCornerShape(30.dp)).background(bg).then(if (action != null) Modifier.clickable(onClick = action) else Modifier).padding(horizontal = 12.dp, vertical = 8.dp))
}
@Composable private fun AppDialog(title: String, sub: String, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = dismiss, containerColor = Paper, title = { Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold) },
        text = { Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text(sub, color = Muted, fontSize = 12.sp); content() } },
        confirmButton = { TextButton(onClick = dismiss) { Text("닫기") } })
}
@Composable private fun Note(text: String) { Text(text, color = Muted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFE8EEE3)).padding(16.dp)) }
@Composable private fun Primary(text: String, enabled: Boolean = true, action: () -> Unit) { Button(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) { Text(text, fontSize = 13.sp) } }
@Composable private fun ToggleRow(title: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), color = Ink, fontSize = 14.sp); Switch(checked, change, colors = SwitchDefaults.colors(checkedTrackColor = Green)) }
}
@Composable private fun Stepper(title: String, value: Int, unit: String, minus: () -> Unit, plus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = minus, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp)) { Text("−", fontSize = 23.sp) }
        Text("$value$unit", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.widthIn(min = 70.dp).padding(horizontal = 12.dp))
        OutlinedButton(onClick = plus, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp)) { Text("+", fontSize = 23.sp) }
    }
}
@Composable private fun ActionRow(title: String, sub: String, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).clickable(onClick = action).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text(sub, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)) }
        Text("↗", color = Green, fontSize = 19.sp)
    }
}

/** Small code-native pictograms keep the dashboard visually consistent at any density. */
@Composable private fun Glyph(kind: String, color: Color, sizeDp: Int) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val s = size.width / 24f
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x*s,y*s), Offset(x2*s,y2*s), 1.65f*s, StrokeCap.Round)
        fun path(vararg points: Pair<Float,Float>, close: Boolean = false) {
            val p = Path().apply { moveTo(points[0].first*s, points[0].second*s); points.drop(1).forEach { lineTo(it.first*s,it.second*s) }; if (close) close() }
            drawPath(p, color, style = Stroke(1.6f*s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        when (kind) {
            "grid" -> for (x in listOf(3f,14f)) for (y in listOf(3f,14f)) drawRoundRect(color, Offset(x*s,y*s), Size(7*s,7*s), androidx.compose.ui.geometry.CornerRadius(s), style = Stroke(1.6f*s))
            "pin" -> { drawCircle(color, 7*s, Offset(12*s,9*s), style = Stroke(1.6f*s)); path(6f to 14f,12f to 22f,18f to 14f); drawCircle(color,2*s,Offset(12*s,9*s)) }
            "sun" -> { drawCircle(color,4*s,center,style=Stroke(1.6f*s)); for(i in 0..7) { val a=i*Math.PI/4; line(12+8*kotlin.math.cos(a).toFloat(),12+8*kotlin.math.sin(a).toFloat(),12+10*kotlin.math.cos(a).toFloat(),12+10*kotlin.math.sin(a).toFloat()) } }
            "moon" -> { val p=Path().apply { moveTo(17*s,3*s); cubicTo(1*s,0f,0f,22*s,16*s,21*s); cubicTo(22*s,20*s,23*s,15*s,22*s,13*s); cubicTo(12*s,18*s,9*s,7*s,17*s,3*s) }; drawPath(p,color,style=Stroke(1.6f*s)) }
            "cup" -> { path(4f to 8f,6f to 21f,16f to 21f,18f to 8f,4f to 8f); line(3f,5f,19f,5f); line(9f,2f,15f,2f); path(18f to 9f,22f to 9f,21f to 15f,17f to 15f) }
            "drop" -> { val p=Path().apply { moveTo(12*s,2*s); cubicTo(9*s,7*s,3*s,12*s,5*s,17*s); cubicTo(8*s,24*s,19*s,22*s,19*s,15*s); cubicTo(19*s,11*s,14*s,5*s,12*s,2*s) }; drawPath(p,color,style=Stroke(1.6f*s)); line(8f,15f,10f,18f) }
            "shield" -> { path(12f to 2f,21f to 6f,19f to 16f,12f to 22f,5f to 16f,3f to 6f,12f to 2f); path(8f to 12f,11f to 15f,16f to 9f) }
            "check" -> path(5f to 12f,10f to 17f,20f to 6f)
            "clock" -> { drawCircle(color,9*s,center,style=Stroke(1.6f*s)); path(12f to 6f,12f to 12f,17f to 15f) }
            "sliders", "settings" -> { for (i in 0..2) { val y=5f+i*7; line(3f,y,21f,y); drawCircle(Paper,2.4f*s,Offset((if(i==1) 15f else 8f)*s,y*s)); drawCircle(color,2.4f*s,Offset((if(i==1) 15f else 8f)*s,y*s),style=Stroke(1.6f*s)) } }
            "leaf" -> { path(3f to 20f,20f to 3f); val p=Path().apply { moveTo(5*s,18*s); cubicTo(0f,4*s,12*s,4*s,21*s,2*s); cubicTo(20*s,16*s,18*s,23*s,5*s,18*s) }; drawPath(p,color,style=Stroke(1.6f*s)) }
            "cycle" -> { drawArc(color,35f,280f,false,Offset(3*s,3*s),Size(18*s,18*s),style=Stroke(1.6f*s)); path(16f to 2f,21f to 5f,16f to 7f) }
            else -> { line(2f,7f,17f,7f); line(5f,12f,22f,12f); line(2f,17f,15f,17f); drawArc(color,180f,180f,false,Offset(14*s,2*s),Size(7*s,5*s),style=Stroke(1.6f*s)) }
        }
    }
}

@Composable private fun CarIllustration(modifier: Modifier) {
    Canvas(modifier) {
        val sx = size.width / 360f; val sy = size.height / 190f
        fun shape(points: List<Pair<Float,Float>>, brush: Brush, stroke: Color? = null) {
            val p = Path().apply { moveTo(points[0].first*sx,points[0].second*sy); points.drop(1).forEach { lineTo(it.first*sx,it.second*sy) }; close() }
            drawPath(p,brush)
            if(stroke != null) drawPath(p,stroke,style=Stroke(.8f*sx))
        }
        drawOval(Color.Black.copy(alpha=.25f),Offset(22*sx,132*sy),Size(326*sx,43*sy))
        fun wheel(x: Float,y:Float) {
            drawOval(Color(0xFF101916),Offset(x*sx,y*sy),Size(43*sx,47*sy))
            drawOval(Brush.linearGradient(listOf(Color(0xFF63736D),Color(0xFF263B32))),Offset((x+8)*sx,(y+6)*sy),Size(27*sx,34*sy))
            drawOval(Color(0xFF101E19),Offset((x+13)*sx,(y+12)*sy),Size(17*sx,22*sy))
            drawLine(Color(0xFF92A39B),Offset((x+21)*sx,(y+10)*sy),Offset((x+21)*sx,(y+36)*sy),2*sx)
        }
        wheel(77f,120f); wheel(274f,110f)
        shape(listOf(20f to 105f,42f to 85f,98f to 72f,135f to 33f,238f to 24f,288f to 57f,325f to 68f,344f to 92f,338f to 133f,305f to 144f,281f to 125f,254f to 130f,116f to 152f,108f to 129f,81f to 123f,67f to 149f,28f to 143f),
            Brush.linearGradient(listOf(Color(0xFFDDE3DC),Color(0xFF8C9D94),Color(0xFF50675B)),Offset(0f,50*sy),Offset(0f,160*sy)),Color(0xFFA9B9AD))
        shape(listOf(105f to 75f,141f to 38f,231f to 30f,271f to 61f),Brush.linearGradient(listOf(Color(0xFF6C8577),Color(0xFF1C3028))))
        shape(listOf(113f to 77f,145f to 43f,184f to 40f,181f to 71f),Brush.linearGradient(listOf(Color(0xFF263E32),Color(0xFF567363))))
        shape(listOf(190f to 39f,228f to 36f,258f to 59f,189f to 69f),Brush.linearGradient(listOf(Color(0xFF263E32),Color(0xFF688372))))
        shape(listOf(24f to 107f,102f to 91f,120f to 111f,29f to 130f),Brush.linearGradient(listOf(Color(0xFFBAC9BF),Color(0xFF647E6B))))
        shape(listOf(105f to 91f,279f to 65f,326f to 76f,120f to 111f),Brush.linearGradient(listOf(Color(0xFFEDF1E8),Color(0xFF9DAF9F))))
        shape(listOf(28f to 112f,100f to 98f,103f to 104f,29f to 120f),Brush.linearGradient(listOf(Lime,Color.White)))
        shape(listOf(29f to 131f,71f to 124f,70f to 135f,30f to 142f),Brush.linearGradient(listOf(Color(0xFF172B21),Color(0xFF33483B))))
        drawLine(Color(0xFF5D7467),Offset(187*sx,79*sy),Offset(187*sx,130*sy),sx)
        drawLine(Color(0xFF3F594B),Offset(214*sx,83*sy),Offset(227*sx,81*sy),2*sx)
        drawLine(Color(0xFFE6EEE1),Offset(120*sx,117*sy),Offset(321*sx,83*sy),sx)
    }
}
