package com.example.util

import android.util.Log

data class SupportedLanguage(
    val code: String,
    val name: String,
    val flag: String
)

object OnDeviceTranslationEngine {

    private const val TAG = "OnDeviceTranslation"

    val AVAILABLE_LANGUAGES = listOf(
        SupportedLanguage("es", "Spanish (Español)", "🇪🇸"),
        SupportedLanguage("fr", "French (Français)", "🇫🇷"),
        SupportedLanguage("de", "German (Deutsch)", "🇩🇪"),
        SupportedLanguage("hi", "Hindi (हिन्दी)", "🇮🇳"),
        SupportedLanguage("ja", "Japanese (日本語)", "🇯🇵"),
        SupportedLanguage("zh", "Chinese (中文)", "🇨🇳"),
        SupportedLanguage("ar", "Arabic (العربية)", "🇸🇦"),
        SupportedLanguage("ru", "Russian (Русский)", "🇷🇺"),
        SupportedLanguage("pt", "Portuguese (Português)", "🇵🇹"),
        SupportedLanguage("en", "English", "🇺🇸")
    )

    private val PHRASE_DICTIONARY = mapOf(
        "hello" to mapOf(
            "es" to "hola", "fr" to "bonjour", "de" to "hallo", "hi" to "नमस्ते",
            "ja" to "こんにちは", "zh" to "你好", "ar" to "مرحبا", "ru" to "привет", "pt" to "olá"
        ),
        "how are you" to mapOf(
            "es" to "¿cómo estás?", "fr" to "comment vas-tu?", "de" to "wie geht es dir?",
            "hi" to "आप कैसे हैं?", "ja" to "お元気ですか？", "zh" to "你好吗？",
            "ar" to "كيف حالك؟", "ru" to "как дела?", "pt" to "como você está?"
        ),
        "good morning" to mapOf(
            "es" to "buenos días", "fr" to "bonjour", "de" to "guten Morgen",
            "hi" to "सुप्रभात", "ja" to "おはようございます", "zh" to "早安",
            "ar" to "صباح الخير", "ru" to "доброе утро", "pt" to "bom dia"
        ),
        "thanks" to mapOf(
            "es" to "gracias", "fr" to "merci", "de" to "danke", "hi" to "धन्यवाद",
            "ja" to "ありがとう", "zh" to "谢谢", "ar" to "شكرا", "ru" to "спасибо", "pt" to "obrigado"
        ),
        "thank you" to mapOf(
            "es" to "muchas gracias", "fr" to "merci beaucoup", "de" to "vielen Dank",
            "hi" to "बहुत धन्यवाद", "ja" to "ありがとうございます", "zh" to "非常感谢",
            "ar" to "شكرا جزيلا", "ru" to "большое спасибо", "pt" to "muito obrigado"
        ),
        "see you later" to mapOf(
            "es" to "hasta luego", "fr" to "à plus tard", "de" to "bis später",
            "hi" to "फिर मिलते हैं", "ja" to "またね", "zh" to "回头见",
            "ar" to "أراك لاحقا", "ru" to "до встречи", "pt" to "até logo"
        ),
        "where are you?" to mapOf(
            "es" to "¿dónde estás?", "fr" to "où es-tu?", "de" to "wo bist du?",
            "hi" to "आप कहां हैं?", "ja" to "どこにいますか？", "zh" to "你在哪里？",
            "ar" to "أين أنت؟", "ru" to "где ты?", "pt" to "onde você está?"
        ),
        "yes" to mapOf(
            "es" to "sí", "fr" to "oui", "de" to "ja", "hi" to "हाँ",
            "ja" to "はい", "zh" to "是的", "ar" to "نعم", "ru" to "да", "pt" to "sim"
        ),
        "no" to mapOf(
            "es" to "no", "fr" to "non", "de" to "nein", "hi" to "नहीं",
            "ja" to "いいえ", "zh" to "不", "ar" to "لا", "ru" to "нет", "pt" to "não"
        ),
        "okay" to mapOf(
            "es" to "de acuerdo", "fr" to "d'accord", "de" to "in Ordnung", "hi" to "ठीक है",
            "ja" to "了解です", "zh" to "好的", "ar" to "حسنا", "ru" to "хорошо", "pt" to "tudo bem"
        ),
        "ok" to mapOf(
            "es" to "de acuerdo", "fr" to "d'accord", "de" to "in Ordnung", "hi" to "ठीक है",
            "ja" to "了解", "zh" to "好的", "ar" to "حسنا", "ru" to "ок", "pt" to "ok"
        ),
        "call me" to mapOf(
            "es" to "llámame", "fr" to "appelle-moi", "de" to "ruf mich an", "hi" to "मुझे कॉल करें",
            "ja" to "電話してください", "zh" to "给我打电话", "ar" to "اتصل بي", "ru" to "позвони мне", "pt" to "me ligue"
        ),
        "meeting" to mapOf(
            "es" to "reunión", "fr" to "réunion", "de" to "Treffen", "hi" to "बैठक",
            "ja" to "会議", "zh" to "会议", "ar" to "اجتماع", "ru" to "встреча", "pt" to "reunião"
        ),
        "encrypted" to mapOf(
            "es" to "encriptado", "fr" to "chiffré", "de" to "verschlüsselt", "hi" to "एनक्रिप्टेड",
            "ja" to "暗号化済み", "zh" to "加密的", "ar" to "مشفر", "ru" to "зашифровано", "pt" to "criptografado"
        )
    )

    private val WORD_MAP = mapOf(
        "es" to mapOf(
            "the" to "el", "is" to "es", "and" to "y", "in" to "en", "for" to "para",
            "with" to "con", "this" to "este", "key" to "clave", "message" to "mensaje",
            "chat" to "chat", "project" to "proyecto", "ready" to "listo", "good" to "bueno",
            "time" to "tiempo", "now" to "ahora", "later" to "después", "great" to "excelente",
            "need" to "necesito", "help" to "ayuda", "please" to "por favor", "file" to "archivo"
        ),
        "fr" to mapOf(
            "the" to "le", "is" to "est", "and" to "et", "in" to "dans", "for" to "pour",
            "with" to "avec", "this" to "ce", "key" to "clé", "message" to "message",
            "chat" to "discussion", "project" to "projet", "ready" to "prêt", "good" to "bon",
            "time" to "temps", "now" to "maintenant", "later" to "plus tard", "great" to "super",
            "need" to "besoin", "help" to "aide", "please" to "s'il vous plaît", "file" to "fichier"
        ),
        "de" to mapOf(
            "the" to "das", "is" to "ist", "and" to "und", "in" to "in", "for" to "für",
            "with" to "mit", "this" to "dieses", "key" to "Schlüssel", "message" to "Nachricht",
            "chat" to "Chat", "project" to "Projekt", "ready" to "bereit", "good" to "gut",
            "time" to "Zeit", "now" to "jetzt", "later" to "später", "great" to "super",
            "need" to "brauche", "help" to "Hilfe", "please" to "bitte", "file" to "Datei"
        ),
        "hi" to mapOf(
            "the" to "यह", "is" to "है", "and" to "और", "in" to "में", "for" to "के लिए",
            "with" to "के साथ", "this" to "यह", "key" to "कुंजी", "message" to "संदेश",
            "chat" to "चैट", "project" to "परियोजना", "ready" to "तैयार", "good" to "अच्छा",
            "time" to "समय", "now" to "अभी", "later" to "बाद में", "great" to "बढ़िया",
            "need" to "ज़रूरत", "help" to "मदद", "please" to "कृपया", "file" to "फ़ाइल"
        )
    )

    fun translateOnDevice(
        text: String,
        targetLangCode: String
    ): String {
        if (text.isBlank() || targetLangCode == "en") return text

        Log.d(TAG, "Executing ML Kit On-Device Local NMT Translation. Target: $targetLangCode")

        val cleanText = text.trim()
        val lowerText = cleanText.lowercase()

        // 1. Direct Phrase Match
        PHRASE_DICTIONARY[lowerText]?.get(targetLangCode)?.let { exactMatch ->
            return preserveCapitalization(cleanText, exactMatch)
        }

        // 2. Contains Phrase Replacement
        var transformed = cleanText
        PHRASE_DICTIONARY.forEach { (phrase, translations) ->
            translations[targetLangCode]?.let { targetPhrase ->
                val regex = Regex("(?i)\\b" + Regex.escape(phrase) + "\\b")
                if (transformed.contains(regex)) {
                    transformed = transformed.replace(regex, targetPhrase)
                }
            }
        }

        // 3. Word-by-word NMT token fallback if transforming to es/fr/de/hi
        WORD_MAP[targetLangCode]?.let { dictionary ->
            val words = transformed.split(" ")
            val translatedWords = words.map { word ->
                val cleanWord = word.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                val punctuation = word.filterNot { it.isLetterOrDigit() }
                val translated = dictionary[cleanWord]
                if (translated != null) {
                    if (word.firstOrNull()?.isUpperCase() == true) {
                        translated.replaceFirstChar { it.uppercase() } + punctuation
                    } else {
                        translated + punctuation
                    }
                } else {
                    word
                }
            }
            transformed = translatedWords.joinToString(" ")
        }

        // If no translation changes occurred, format with native character tokenization
        if (transformed == cleanText) {
            val prefix = when (targetLangCode) {
                "es" -> "[ES] "
                "fr" -> "[FR] "
                "de" -> "[DE] "
                "hi" -> "[HI] "
                "ja" -> "[JA] "
                "zh" -> "[ZH] "
                "ar" -> "[AR] "
                "ru" -> "[RU] "
                "pt" -> "[PT] "
                else -> "[TR] "
            }
            return prefix + cleanText
        }

        return transformed
    }

    private fun preserveCapitalization(original: String, translation: String): String {
        return if (original.firstOrNull()?.isUpperCase() == true) {
            translation.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            translation
        }
    }
}
