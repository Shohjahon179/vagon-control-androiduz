package uz.vagoncontrol

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

val Navy = Color(0xFF0B1F3A)
val Navy2 = Color(0xFF16345F)
val Gold = Color(0xFFD4AF37)
val Cream = Color(0xFFF5F1E6)
val Danger = Color(0xFFFF8A80)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent { App(store) }
    }
}

@Composable
fun App(store: Store) {
    var screen by rememberSaveable { mutableStateOf("auth") }
    var user by rememberSaveable { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, onPrimary = Navy)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Navy, Navy2)))
            ) {
                BackHandler(enabled = screen == "worker" || screen == "boss") { screen = "role" }
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        (fadeIn(tween(450)) + slideInHorizontally(tween(450)) { it / 4 }) togetherWith fadeOut(tween(250))
                    },
                    label = "nav"
                ) { s ->
                    when (s) {
                        "auth" -> AuthScreen(store) { login -> user = login; screen = "role" }
                        "role" -> RoleScreen(
                            onWorker = { screen = "worker" },
                            onBoss = { screen = "boss" },
                            onLogout = { user = ""; screen = "auth" }
                        )
                        "worker" -> WorkerScreen(store, user) { screen = "role" }
                        else -> BossScreen(store) { screen = "role" }
                    }
                }
            }
        }
    }
}

// ───────────── Umumiy maydon ─────────────

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    prefix: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            color = Gold,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text(hint) },
            prefix = if (prefix != null) ({ Text(prefix) }) else null,
            trailingIcon = trailing,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Gold,
                unfocusedBorderColor = Color(0x66FFFFFF),
                cursorColor = Gold,
                focusedPlaceholderColor = Color(0x66FFFFFF),
                unfocusedPlaceholderColor = Color(0x44FFFFFF),
                focusedPrefixColor = Color.White,
                unfocusedPrefixColor = Color.White
            )
        )
    }
}

fun fmtDate(s: String): String {
    val d = s.filter { it.isDigit() }.take(8)
    val sb = StringBuilder()
    d.forEachIndexed { i, c ->
        if (i == 2 || i == 4) sb.append('.')
        sb.append(c)
    }
    return sb.toString()
}

fun fmtTime(s: String): String {
    val d = s.filter { it.isDigit() }.take(4)
    val sb = StringBuilder()
    d.forEachIndexed { i, c ->
        if (i == 2) sb.append(':')
        sb.append(c)
    }
    return sb.toString()
}

fun pick(ctx: Context, kind: Int, set: (String) -> Unit) {
    val c = Calendar.getInstance()
    if (kind == 1) {
        DatePickerDialog(
            ctx,
            { _, y, m, d -> set("%02d.%02d.%04d".format(d, m + 1, y)) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    } else {
        TimePickerDialog(
            ctx,
            { _, h, m -> set("%02d:%02d".format(h, m)) },
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true
        ).show()
    }
}

// ───────────── 1. Kirish / Ro'yxatdan o'tish ─────────────

@Composable
fun AuthScreen(store: Store, onDone: (String) -> Unit) {
    var reg by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "scale"
    )

    fun submit() {
        err = ""
        val l = login.trim()
        if (reg) {
            if (phone.length != 9) { err = "Telefon raqamni to'liq kiriting (9 ta raqam)"; return }
            if (l.length < 3) { err = "Login kamida 3 ta belgi bo'lsin"; return }
            if (pass.length < 6) { err = "Parol kamida 6 ta belgi bo'lsin"; return }
            if (pass != pass2) { err = "Parollar bir xil emas"; return }
            val e = store.register("+998$phone", l, pass)
            if (e != null) { err = e; return }
            onDone(l)
        } else {
            if (store.login(l, pass)) onDone(l) else err = "Login yoki parol noto'g'ri"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("🚆", fontSize = 64.sp, modifier = Modifier.scale(pulse))
        Spacer(Modifier.height(8.dp))
        Text("VAGON CONTROL", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text("Vagonlar hisobi", color = Cream, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().animateContentSize(tween(350))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (reg) "Ro'yxatdan o'tish" else "Kirish",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(visible = reg) {
                    Field(
                        label = "Telefon raqam",
                        value = phone,
                        onChange = { phone = it.filter { c -> c.isDigit() }.take(9) },
                        hint = "90 123 45 67",
                        keyboard = KeyboardType.Phone,
                        prefix = "+998 "
                    )
                }
                Field("Login", login, { login = it.replace(" ", "") }, hint = "login kiriting")
                Field("Parol", pass, { pass = it }, hint = "parol kiriting", password = true)
                AnimatedVisibility(visible = reg) {
                    Field("Parolni qayta kiriting", pass2, { pass2 = it }, hint = "parolni takrorlang", password = true)
                }

                AnimatedVisibility(visible = err.isNotEmpty()) {
                    Text(err, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { submit() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy)
                ) {
                    Text(if (reg) "Ro'yxatdan o'tish" else "Kirish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                TextButton(
                    onClick = { reg = !reg; err = "" },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (reg) "Akkauntim bor — kirish" else "Akkaunt yo'qmi? Ro'yxatdan o'ting",
                        color = Cream
                    )
                }
            }
        }
    }
}

// ───────────── 2. Lavozim tanlash ─────────────

@Composable
fun RoleScreen(onWorker: () -> Unit, onBoss: () -> Unit, onLogout: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🚆", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Lavozimingizni tanlang",
                color = Gold,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        TextButton(onClick = onLogout, modifier = Modifier.align(Alignment.TopEnd)) {
            Text("Chiqish", color = Cream)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = shown,
                modifier = Modifier.weight(1f),
                enter = slideInVertically(tween(600)) { it } + fadeIn(tween(600))
            ) { RoleButton("👷", "Ishchi", true, onWorker) }
            AnimatedVisibility(
                visible = shown,
                modifier = Modifier.weight(1f),
                enter = slideInVertically(tween(600, delayMillis = 150)) { it } + fadeIn(tween(600, delayMillis = 150))
            ) { RoleButton("👔", "Boshliq", false, onBoss) }
        }
    }
}

@Composable
fun RoleButton(icon: String, title: String, filled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(130.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) Gold else Cream,
            contentColor = Navy
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 36.sp)
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ───────────── 3. Ishchi: vagon kiritish ─────────────

@Composable
fun WorkerScreen(store: Store, user: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val f = remember { mutableStateListOf(*Array(11) { "" }) }
    var err by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Orqaga", color = Cream) }
            Spacer(Modifier.weight(1f))
            Text("👷 $user", color = Gold, fontWeight = FontWeight.SemiBold)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Text("Vagon ma'lumotlari", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            LABELS.forEachIndexed { i, label ->
                val kind = KINDS[i]
                Field(
                    label = "${i + 1}. $label",
                    value = f[i],
                    onChange = {
                        f[i] = when (kind) {
                            1 -> fmtDate(it)
                            2 -> fmtTime(it)
                            else -> it
                        }
                    },
                    hint = when (kind) {
                        1 -> "kun.oy.yil"
                        2 -> "soat:daqiqa"
                        else -> label
                    },
                    keyboard = if (kind == 0) KeyboardType.Text else KeyboardType.Number,
                    trailing = if (kind == 0) null else ({
                        TextButton(onClick = { pick(ctx, kind) { v -> f[i] = v } }) {
                            Text(if (kind == 1) "📅" else "🕒", fontSize = 20.sp)
                        }
                    })
                )
            }

            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize(tween(300))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Vagon turgan vaqt", color = Cream, fontSize = 13.sp)
                    Text(stayText(f.toList()), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = err.isNotEmpty()) {
                Text(err, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val e = validateRecord(f.toList())
                    if (e != null) {
                        err = e
                    } else {
                        err = ""
                        store.save(f.toList(), user)
                        Toast.makeText(ctx, "Saqlandi", Toast.LENGTH_SHORT).show()
                        for (k in f.indices) f[k] = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy)
            ) {
                Text("Saqlash", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ───────────── 4. Boshliq: avtomatik jadval ─────────────

@Composable
fun BossScreen(store: Store, onBack: () -> Unit) {
    val rows = remember { store.records() }
    val headers = listOf("№") + LABELS + listOf("Turgan vaqti", "Kiritdi")
    val widths = listOf(44) + List(11) { 118 } + listOf(150, 100)

    val minutes = rows.mapNotNull { stayMinutes(it) }
    val avg = if (minutes.isEmpty()) "—" else minutesText(minutes.sum() / minutes.size)
    val waiting = rows.count { it[8].isEmpty() }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Orqaga", color = Cream) }
            Spacer(Modifier.weight(1f))
            Text("👔 Boshliq", color = Gold, fontWeight = FontWeight.SemiBold)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Jami vagon", rows.size.toString(), Modifier.weight(1f))
            StatCard("Hali turibdi", waiting.toString(), Modifier.weight(1f))
            StatCard("O'rtacha turish", avg, Modifier.weight(1.4f))
        }
        Spacer(Modifier.height(12.dp))

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Hali ma'lumot yo'q. Ishchi vagon kiritgach, jadval shu yerda o'zi to'ladi.",
                    color = Cream,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                Column {
                    Row(Modifier.background(Gold)) {
                        headers.forEachIndexed { i, h ->
                            Text(
                                h,
                                color = Navy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(widths[i].dp).padding(horizontal = 6.dp, vertical = 12.dp)
                            )
                        }
                    }
                    LazyColumn {
                        itemsIndexed(rows) { idx, r ->
                            val cells = listOf((idx + 1).toString()) + r.take(11) + listOf(stayText(r), r.getOrElse(11) { "" })
                            Row(Modifier.background(if (idx % 2 == 0) Color(0x14FFFFFF) else Color.Transparent)) {
                                cells.forEachIndexed { i, c ->
                                    Text(
                                        c.ifEmpty { "—" },
                                        color = if (i == 12) Gold else Color.White,
                                        fontWeight = if (i == 12) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(widths[i].dp).padding(horizontal = 6.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = Cream, fontSize = 11.sp)
            Text(value, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
