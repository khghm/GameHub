package com.gamehub.games.esmofamil

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

val ESF_BOT_VOCAB = mapOf(
    'ا' to listOf("علی", "احمدی", "اراک", "اردن", "انار", "اسب", "استاد", "اتبالی"),
    'ب' to listOf("بابک", "بابایی", "بوشهر", "برزیل", "برتقال", "بلبل", "بیکر", "بسکتبال"),
    'پ' to listOf("پریسا", "پورمحمدی", "پروی", "پرتغال", "پسته", "پاندا", "پزشک", "پیشوازی"),
    'ت' to listOf("تارا", "تجریشی", "تهران", "تایلند", "توت", "تروتیر", "تکنسین", "تکواندو"),
    'ث' to listOf("ثریا", "ثریایی", "ثمر", "تالاب", "ثمرات", "ثور", "ثالج", "ثانیه"),
    'ج' to listOf("جاوید", "جاویدی", "جلفا", "جزیر", "جی", "جیر", "جنگجو", "جودو"),
    'چ' to listOf("چیرا", "چراغی", "چاهک", "چین", "چاق", "چکاوک", "چکار", "چوگان"),
    'ح' to listOf("حسین", "حسینی", "حمید", "هند", "حبوبات", "حمار", "حکیم", "حرف بازی"),
    'خ' to listOf("خاشایار", "خشایار", "خوزستان", "خورشید", "خیار", "خرس", "خبرنگار", "خوراکی"),
    'د' to listOf("داریوش", "داریوشی", "دزفول", "دانمارک", "دانه", "دیگ", "دانشجو", "دوچرخه سواری"),
    'ذ' to listOf("ذوالفقار", "ذوالفقاری", "ذوق", "ذیل", "ذغال", "ذراع", "ذوق‌آفرین", "ذوق‌آفرینی"),
    'ر' to listOf("رضا", "رضایی", "رشت", "روسیه", "راز", "رنگ‌آمیزی", "رنگ‌آمیزیگر", "رزمی"),
    'ز' to listOf("زهرا", "زهرایی", "زنجان", "زیمباوه", "زردآلو", "زردپرست", "زندانی", "زومبی"),
    'ژ' to listOf("ژاله", "ژالایی", "ژانوی", "ژاپن", "ژاله", "ژاپونی", "ژنتیک", "ژوژیتو"),
    'س' to listOf("سامان", "سامانی", "ساری", "سوئیس", "سیب", "سیاه‌پشت", "سرباز", "سواری"),
    'ش' to listOf("شهرام", "شهرامی", "شیراز", "سوئد", "شلیل", "شیر", "شعبان", "شطرنج"),
    'ص' to listOf("صابر", "صابری", "صحنه", "صحرا", "صندلی", "صهیون", "صحاب", "صحیح"),
    'ض' to listOf("ضیا", "ضیایی", "ضحک", "ضحاک", "ضوابط", "ضوء", "ضحک", "ضحاکی"),
    'ط' to listOf("طاها", "طاهایی", "طبرستان", "تایلند", "طالسی", "طاووس", "طبیب", "طبیب‌بازی"),
    'ظ' to listOf("ظفر", "ظفری", "ظهران", "تایلند", "ظریف", "ظفر", "ظریف", "ظریفی"),
    'ع' to listOf("عرفان", "عرفانی", "عسلویه", "عراق", "عناب", "عقاب", "عاشق", "عاشقانه"),
    'غ' to listOf("غلام", "غلامی", "غوچان", "غنا", "غزال", "غلام", "غواص", "غواصی"),
    'ف' to listOf("فرزاد", "فرزادی", "فارس", "فرانسه", "فلفل", "فیل", "فامیل", "فوتبال"),
    'ق' to listOf("قاسم", "قاسمی", "قشم", "قطر", "قیر", "قوچی", "قاب", "قاپا"),
    'ک' to listOf("کیان", "کیانی", "کرمان", "کانادا", "کالا", "کلاغ", "کارمند", "کشتی"),
    'گ' to listOf("گزاره", "گزاره‌ای", "گرگان", "گینه", "گلابی", "گربه", "گارسون", "گلف"),
    'ل' to listOf("لیلا", "لیلی", "لاهیجان", "لوگان", "لوما", "لک", "لشکرکش", "لژی"),
    'م' to listOf("مریم", "مریمی", "مشهد", "مكزیك", "موز", "موش", "مدرس", "موتور"),
    'ن' to listOf("ناصر", "ناصری", "نیشابور", "نروژ", "نارنگ", "نرگس", "نظیر", "نوشیدنی"),
    'و' to listOf("وحید", "وحیدی", "ورزقان", "اوزبکستان", "واو", "ویژن", "وکیل", "وومن"),
    'ه' to listOf("هادی", "هادیان", "همدان", "هند", "هلو", "هستم", "هنرآموز", "هکی"),
    'ی' to listOf("یاسمن", "یاسمنی", "یزد", "یونان", "یزدی", "یوزپلنگ", "یوتیوپر", "یوکس")
)

class EsmoFamilBotStrategy : BotStrategy<EsmoFamilState, EsmoFamilAction> {
    override val gameId: String = EsmoFamilState.GAME_ID
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(
        state: EsmoFamilState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): EsmoFamilAction? {
        if (state.phase != EsmoFamilPhase.ANSWERING) {
            return null
        }

        if (state.playerAnswers.containsKey(botPlayerId) &&
            state.playerAnswers[botPlayerId]?.answers?.isNotEmpty() == true) {
            return null
        }

        val delayMs = when {
            difficultyLevel <= 3 -> 2000L
            difficultyLevel <= 6 -> 4000L
            else -> 8000L
        }
        delay(delayMs)

        val vocab = ESF_BOT_VOCAB[state.currentLetter] ?: ESF_BOT_VOCAB.values.flatten().shuffled()
        val answers = mutableMapOf<Int, String?>()

        // First, ensure we have at least one answer
        val minAnswers = when {
            difficultyLevel <= 2 -> 1
            difficultyLevel <= 5 -> 3
            else -> ESF_CATEGORIES.size
        }

        // Add guaranteed answers
        val guaranteedIndices = (0 until ESF_CATEGORIES.size).shuffled().take(minAnswers)
        for (categoryIdx in guaranteedIndices) {
            answers[categoryIdx] = vocab.getOrNull(categoryIdx % vocab.size)
        }

        // Then add optional answers based on difficulty
        for (categoryIdx in ESF_CATEGORIES.indices) {
            if (!answers.containsKey(categoryIdx) && Random.nextInt(1, 11) <= difficultyLevel) {
                answers[categoryIdx] = vocab.getOrNull(categoryIdx % vocab.size)
            }
        }

        return EsmoFamilAction.SubmitAnswers(answers)
    }
}
