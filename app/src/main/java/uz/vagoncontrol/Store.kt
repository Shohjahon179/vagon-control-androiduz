package uz.vagoncontrol

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

val LABELS = listOf(
    "Vagon nomeri",
    "Kirib kelgan sanasi",
    "Kirib kelgan vaqti",
    "Poezd nomeri",
    "Berilgan sana",
    "Berilgan vaqti",
    "Olingan sana",
    "Olingan vaqti",
    "Jo'nash sanasi",
    "Jo'nash vaqti",
    "Poezd raqami"
)

// 0 = oddiy matn, 1 = sana, 2 = vaqt
val KINDS = listOf(0, 1, 2, 0, 1, 2, 1, 2, 1, 2, 0)

private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).apply { isLenient = false }

fun parseDateTime(d: String, t: String): Long? {
    if (d.length != 10 || t.length != 5) return null
    return try {
        dateTimeFormat.parse("$d $t")?.time
    } catch (e: Exception) {
        null
    }
}

/** Vagon necha daqiqa turgani: kirib kelgan vaqtdan jo'nash vaqtigacha. */
fun stayMinutes(r: List<String>): Long? {
    val a = parseDateTime(r[1], r[2]) ?: return null
    val b = parseDateTime(r[8], r[9]) ?: return null
    if (b < a) return null
    return (b - a) / 60000
}

fun minutesText(m: Long): String = "${m / 60} soat ${m % 60} daq"

fun stayText(r: List<String>): String {
    if (parseDateTime(r[1], r[2]) == null) return "—"
    if (parseDateTime(r[8], r[9]) == null) return "Hali turibdi"
    return stayMinutes(r)?.let { minutesText(it) } ?: "Xato"
}

fun validateRecord(r: List<String>): String? {
    if (r[0].isBlank()) return "Vagon nomerini kiriting"
    val pairs = listOf(1 to 2, 4 to 5, 6 to 7, 8 to 9)
    for ((a, b) in pairs) {
        val empty = r[a].isBlank() && r[b].isBlank()
        if (a == 1 && empty) return "Kirib kelgan sana va vaqtni kiriting"
        if (!empty && parseDateTime(r[a], r[b]) == null) {
            return "\"${LABELS[a]}\" va \"${LABELS[b]}\" to'g'ri emas (kun.oy.yil va soat:daqiqa)"
        }
    }
    val t1 = parseDateTime(r[1], r[2])
    val t2 = parseDateTime(r[8], r[9])
    if (t1 != null && t2 != null && t2 < t1) return "Jo'nash vaqti kirib kelgan vaqtdan oldin bo'lishi mumkin emas"
    return null
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("vagon_control", Context.MODE_PRIVATE)

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun users(): JSONArray = JSONArray(sp.getString("users", "[]"))

    /** Xato bo'lsa matn qaytaradi, muvaffaqiyatli bo'lsa null. */
    fun register(phone: String, login: String, pass: String): String? {
        val arr = users()
        for (i in 0 until arr.length()) {
            val u = arr.getJSONObject(i)
            if (u.getString("login").equals(login, ignoreCase = true)) return "Bu login band"
            if (u.getString("phone") == phone) return "Bu raqam allaqachon ro'yxatdan o'tgan"
        }
        arr.put(JSONObject().put("phone", phone).put("login", login).put("pass", hash(pass)))
        sp.edit().putString("users", arr.toString()).apply()
        return null
    }

    fun login(login: String, pass: String): Boolean {
        val arr = users()
        for (i in 0 until arr.length()) {
            val u = arr.getJSONObject(i)
            if (u.getString("login").equals(login, ignoreCase = true) && u.getString("pass") == hash(pass)) return true
        }
        return false
    }

    /** Har bir yozuv: 11 ta maydon + 12-chi element: kim kiritgani. */
    fun records(): List<List<String>> {
        val arr = JSONArray(sp.getString("records", "[]"))
        val out = ArrayList<List<String>>()
        for (i in 0 until arr.length()) {
            val a = arr.getJSONArray(i)
            out.add(List(a.length()) { a.getString(it) })
        }
        return out
    }

    fun save(fields: List<String>, owner: String) {
        val all = records().toMutableList()
        val rec = fields.map { it.trim() } + owner
        // Shu vagon hali jo'natilmagan bo'lsa — yangi qator emas, o'sha qator yangilanadi
        val idx = all.indexOfFirst { it[0] == rec[0] && it[8].isEmpty() }
        if (idx >= 0) all[idx] = rec else all.add(rec)
        val arr = JSONArray()
        all.forEach { r -> arr.put(JSONArray(r)) }
        sp.edit().putString("records", arr.toString()).apply()
    }
}
