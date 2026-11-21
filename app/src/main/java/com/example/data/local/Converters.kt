package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PollData
import com.example.data.model.PollOption
import com.example.data.model.PostRevision
import com.example.data.model.VerificationBadge
import org.json.JSONArray
import org.json.JSONObject

class XProTypeConverters {

    @TypeConverter
    fun fromVerificationBadge(badge: VerificationBadge): String = badge.name

    @TypeConverter
    fun toVerificationBadge(value: String): VerificationBadge =
        runCatching { VerificationBadge.valueOf(value) }.getOrDefault(VerificationBadge.NONE)

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index -> array.optString(index, "") }.filter { it.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromRevisions(revisions: List<PostRevision>): String {
        val array = JSONArray()
        revisions.forEach { rev ->
            array.put(
                JSONObject().apply {
                    put("content", rev.content)
                    put("editedAtMillis", rev.editedAtMillis)
                    put("reason", rev.reason)
                }
            )
        }
        return array.toString()
    }

    @TypeConverter
    fun toRevisions(json: String): List<PostRevision> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { idx ->
                val obj = array.getJSONObject(idx)
                PostRevision(
                    content = obj.optString("content", ""),
                    editedAtMillis = obj.optLong("editedAtMillis", 0L),
                    reason = obj.optString("reason", "Updated post")
                )
            }
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromPollData(poll: PollData?): String? {
        if (poll == null) return null
        val optionsArray = JSONArray()
        poll.options.forEach { opt ->
            optionsArray.put(
                JSONObject().apply {
                    put("id", opt.id)
                    put("label", opt.label)
                    put("votes", opt.votes)
                }
            )
        }
        return JSONObject().apply {
            put("question", poll.question)
            put("selectedOptionId", poll.selectedOptionId ?: JSONObject.NULL)
            put("durationLabel", poll.durationLabel)
            put("options", optionsArray)
        }.toString()
    }

    @TypeConverter
    fun toPollData(json: String?): PollData? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(json)
            val optArray = obj.getJSONArray("options")
            val options = List(optArray.length()) { idx ->
                val o = optArray.getJSONObject(idx)
                PollOption(
                    id = o.getString("id"),
                    label = o.getString("label"),
                    votes = o.getInt("votes")
                )
            }
            val selected = if (obj.isNull("selectedOptionId")) null else obj.optString("selectedOptionId")
            PollData(
                question = obj.optString("question", ""),
                options = options,
                selectedOptionId = selected,
                durationLabel = obj.optString("durationLabel", "24h remaining")
            )
        }.getOrNull()
    }
}
