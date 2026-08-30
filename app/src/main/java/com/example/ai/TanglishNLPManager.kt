package com.example.ai

import java.util.Locale

/**
 * Text normalization and transliteration layer for English, Tamil Unicode,
 * and Tanglish (Tamil written in Latin script, e.g., "vanakkam", "eppadi irukkinga").
 */
object TanglishNLPManager {

    private val tanglishToTamilMap: Map<String, String> = mapOf(
        "vanakkam" to "வணக்கம்",
        "kalai vanakkam" to "காலை வணக்கம்",
        "eppadi irukkinga" to "எப்படி இருக்கீங்க",
        "epdi irukinga" to "எப்படி இருக்கீங்க",
        "nandri" to "நன்றி",
        "sari" to "சரி",
        "poittu varren" to "போயிட்டு வர்றேன்",
        "varukiren" to "வருகிறேன்",
        "enna pannuringa" to "என்ன பண்றீங்க",
        "saaptaacha" to "சாப்பிாச்சா",
        "veetla ellaarum sowkyama" to "வீட்ல எல்லாரும் சௌக்கியமா",
        "naalai" to "நாளை",
        "inru" to "இன்று",
        "iniki" to "இன்னைக்கு",
        "naaliki" to "நாளைக்கு",
        "remind pannu" to "நினைவூட்டு",
        "calender la add pannu" to "நாட்காட்டியில் சேர்",
        "meeting irukku" to "கூட்டம் இருக்கிறது",
        "time enna" to "நேரம் என்ன"
    )

    fun detectLanguage(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "EN"
        
        // Check for Tamil Unicode range (U+0B80 - U+0BFF)
        val containsTamilUnicode = trimmed.any { it.code in 0x0B80..0x0BFF }
        if (containsTamilUnicode) return "TA_UNICODE"

        // Check for Tanglish keywords
        val lower = trimmed.lowercase(Locale.ROOT)
        val containsTanglish = tanglishToTamilMap.keys.any { lower.contains(it) }
        if (containsTanglish) return "TANGLISH"

        return "EN"
    }

    fun normalizeAndTransliterate(input: String): String {
        val lang = detectLanguage(input)
        if (lang != "TANGLISH") return input.trim()

        var normalized = input.lowercase(Locale.ROOT)
        tanglishToTamilMap.forEach { (tanglish, tamil) ->
            normalized = normalized.replace(tanglish, tamil)
        }
        return normalized.trim()
    }

    data class IntentResult(
        val originalText: String,
        val normalizedText: String,
        val detectedLanguage: String,
        val detectedIntent: String,
        val extractedEntities: Map<String, String>
    )

    fun extractIntent(input: String): IntentResult {
        val normalized = normalizeAndTransliterate(input)
        val lang = detectLanguage(input)
        val lower = input.lowercase(Locale.ROOT)

        val intent = when {
            lower.contains("remind") || lower.contains("reminder") || lower.contains("ninai") || lower.contains("gnyabagam") -> "EXTRACT_REMINDER"
            lower.contains("calendar") || lower.contains("meeting") || lower.contains("event") || lower.contains("schedule") -> "SUGGEST_CALENDAR_EVENT"
            lower.contains("when did i say") || lower.contains("who did i tell") || lower.contains("search") || lower.contains("find") -> "MEMORY_SEARCH"
            lower.contains("summarize") || lower.contains("summary") || lower.contains("crux") -> "CONVERSATION_SUMMARY"
            lower.contains("scam") || lower.contains("otp") || lower.contains("bank") || lower.contains("urgent money") -> "SCAM_DETECTION"
            lower.contains("reply") || lower.contains("suggest") || lower.contains("write") -> "SMART_REPLY"
            else -> "GENERAL_ASSISTANT_QUERY"
        }

        val entities = mutableMapOf<String, String>()
        if (lower.contains("tomorrow") || lower.contains("naaliki")) {
            entities["date"] = "Tomorrow"
        } else if (lower.contains("today") || lower.contains("iniki")) {
            entities["date"] = "Today"
        }

        return IntentResult(
            originalText = input,
            normalizedText = normalized,
            detectedLanguage = lang,
            detectedIntent = intent,
            extractedEntities = entities
        )
    }
}
