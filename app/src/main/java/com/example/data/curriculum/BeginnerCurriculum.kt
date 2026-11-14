package com.example.data.curriculum

import com.example.data.model.Lesson
import com.example.data.model.LessonPhrase

object BeginnerCurriculum {
    val lessons: List<Lesson> = listOf(
        Lesson(
            id = "greetings",
            number = 1,
            titleEnglish = "Greetings & Introductions",
            titleArabic = "التحيات والتعارف",
            subtitleArabic = "كيف تحيّي الناس وتعرّف بنفسك بلطف",
            focusTipArabic = "في الإنجليزية نقول My name is (اسمي يكون) — ولا ننسى فعل الكينونة is.",
            xpReward = 50L,
            phrases = listOf(
                LessonPhrase(
                    english = "Hello! My name is Omar.",
                    arabic = "مرحباً! اسمي عمر.",
                    phoneticArabic = "هيلو! ماي نيم إز عُمر",
                    tipArabic = "استخدمها للتعريف بنفسك في أي وقت."
                ),
                LessonPhrase(
                    english = "Nice to meet you.",
                    arabic = "تشرفت بلقائك.",
                    phoneticArabic = "نايس تو ميت يو",
                    tipArabic = "تُقال عندما تقابل شخصاً للمرة الأولى."
                ),
                LessonPhrase(
                    english = "How are you today?",
                    arabic = "كيف حالك اليوم؟",
                    phoneticArabic = "هاو آر يو توداي؟",
                    tipArabic = "الرد الشائع: I am fine, thank you."
                ),
                LessonPhrase(
                    english = "I am from Egypt.",
                    arabic = "أنا من مصر.",
                    phoneticArabic = "آي آم فروم إيجيبت",
                    tipArabic = "استخدم I am دائماً قبل البلد أو الصفة."
                )
            )
        ),
        Lesson(
            id = "p_b_v_f_sounds",
            number = 2,
            titleEnglish = "The P/B & V/F Sounds",
            titleArabic = "تمييز أصوات P و B و V و F",
            subtitleArabic = "أصوات غير موجودة في العربية وتغير المعنى",
            focusTipArabic = "حرف P يخرج مع دفعة هواء قوية (مثل Paper)، أما B فيخرج بدون هواء قوي. وحرف V يهتز فيه الحلق مثل ذبذبة خفيفة.",
            xpReward = 60L,
            phrases = listOf(
                LessonPhrase(
                    english = "Please pass the pen.",
                    arabic = "من فضلك مرر القلم.",
                    phoneticArabic = "پليز پاس ذَ پين",
                    tipArabic = "انتبه لنفخة الهواء في حرف P في Please و Pen."
                ),
                LessonPhrase(
                    english = "A very big village.",
                    arabic = "قرية كبيرة جداً.",
                    phoneticArabic = "أَ فيري بيغ فيليدج",
                    tipArabic = "حرف V يهتز في الشفة السفلى مع الأسنان العليا."
                ),
                LessonPhrase(
                    english = "Park the blue bus.",
                    arabic = "اركن الحافلة الزرقاء.",
                    phoneticArabic = "پارك ذَ بلو باس",
                    tipArabic = "Park تبدأ بـ P، و Bus تبدأ بـ B."
                ),
                LessonPhrase(
                    english = "Five fresh apples.",
                    arabic = "خمس تفاحات طازجة.",
                    phoneticArabic = "فايف فريش آپلز",
                    tipArabic = "F في Five بدون اهتزاز، و V في نهايتها باهتزاز."
                )
            )
        ),
        Lesson(
            id = "verb_to_be",
            number = 3,
            titleEnglish = "Mastering Am, Is, Are",
            titleArabic = "سرّ الجملة الإنجليزية: Am / Is / Are",
            subtitleArabic = "لماذا نضيف كلمة (يكون) في المضارع؟",
            focusTipArabic = "في العربية نقول (أنا سعيد) بدون فعل، لكن الإنجليزية تشترط وجود فعل: I am happy و He is happy.",
            xpReward = 60L,
            phrases = listOf(
                LessonPhrase(
                    english = "I am ready to learn.",
                    arabic = "أنا مستعد للتعلّم.",
                    phoneticArabic = "آي آم ريدي تو ليرن",
                    tipArabic = "I تأتي دائماً مع am."
                ),
                LessonPhrase(
                    english = "She is a good teacher.",
                    arabic = "هي معلمة جيدة.",
                    phoneticArabic = "شي إز أَ غود تيتشر",
                    tipArabic = "He / She / It تأتي دائماً مع is."
                ),
                LessonPhrase(
                    english = "We are at home now.",
                    arabic = "نحن في المنزل الآن.",
                    phoneticArabic = "وي آر آت هوم ناو",
                    tipArabic = "You / We / They تأتي دائماً مع are."
                ),
                LessonPhrase(
                    english = "Is the coffee hot?",
                    arabic = "هل القهوة ساخنة؟",
                    phoneticArabic = "إز ذَ كوفي هوت؟",
                    tipArabic = "للسؤال نضع Is أو Are في بداية الجملة."
                )
            )
        ),
        Lesson(
            id = "cafe_shopping",
            number = 4,
            titleEnglish = "At the Café & Shopping",
            titleArabic = "في المقهى والتسوق",
            subtitleArabic = "اطلب طعامك واسأل عن السعر بأدب",
            focusTipArabic = "عبارة Can I have... تعني (هل يمكنني الحصول على...) وهي ألطف بكثير من قول Give me.",
            xpReward = 70L,
            phrases = listOf(
                LessonPhrase(
                    english = "Can I have a tea, please?",
                    arabic = "هل يمكنني الحصول على شاي من فضلك؟",
                    phoneticArabic = "كان آي هاف أَ تي، پليز؟",
                    tipArabic = "إضافة please في النهاية تجعل الطلب مهذباً جداً."
                ),
                LessonPhrase(
                    english = "How much is this?",
                    arabic = "كم سعر هذا؟",
                    phoneticArabic = "هاو ماتش إز ذِس؟",
                    tipArabic = "استخدمها للسؤال عن سعر أي شيء مفرد."
                ),
                LessonPhrase(
                    english = "I would like some water.",
                    arabic = "أودّ بعض الماء.",
                    phoneticArabic = "آي وود لايك سام ووتر",
                    tipArabic = "حرف L في would صامت ولا يُنطق."
                ),
                LessonPhrase(
                    english = "Thank you very much!",
                    arabic = "شكراً جزيلاً لك!",
                    phoneticArabic = "ثانك يو فيري ماتش",
                    tipArabic = "صوت TH هنا مثل حرف الثاء في العربية."
                )
            )
        ),
        Lesson(
            id = "articles_a_an_the",
            number = 5,
            titleEnglish = "Using A, An, and The",
            titleArabic = "أدوات التعريف والتنكير A / An / The",
            subtitleArabic = "متى نضع A ومتى نضع An أو The؟",
            focusTipArabic = "في العربية توجد (الـ) للتعريف ولا توجد أداة للنكرة. في الإنجليزية المفرد النكرة يحتاج A (أو An قبل الأصوات المتحركة مثل apple).",
            xpReward = 65L,
            phrases = listOf(
                LessonPhrase(
                    english = "I have a new book.",
                    arabic = "لدي كتاب جديد.",
                    phoneticArabic = "آي هاف أَ نيو بوك",
                    tipArabic = "book اسم مفرد نكرة يبدأ بصوت ساكن فنضع a."
                ),
                LessonPhrase(
                    english = "She wants an apple.",
                    arabic = "هي تريد تفاحة.",
                    phoneticArabic = "شي وونتس آن آپل",
                    tipArabic = "نضع an لأن apple تبدأ بصوت متحرك (آ)."
                ),
                LessonPhrase(
                    english = "The sun is bright today.",
                    arabic = "الشمس ساطعة اليوم.",
                    phoneticArabic = "ذَ سان إز برايت توداي",
                    tipArabic = "The تقابل (الـ) التعريف في العربية."
                ),
                LessonPhrase(
                    english = "An hour and a half.",
                    arabic = "ساعة ونصف.",
                    phoneticArabic = "آن آوَر آند أَ هاف",
                    tipArabic = "حرف H في hour صامت، لذلك ننطقها بصوت متحرك ونستخدم an."
                )
            )
        ),
        Lesson(
            id = "asking_questions",
            number = 6,
            titleEnglish = "Essential Questions",
            titleArabic = "تكوين الأسئلة اليومية",
            subtitleArabic = "What, Where, When, Why, How",
            focusTipArabic = "ابدأ بأداة الاستفهام ثم الفعل المساعد (do أو is)، مثلاً: Where do you live?",
            xpReward = 75L,
            phrases = listOf(
                LessonPhrase(
                    english = "Where do you live?",
                    arabic = "أين تسكن؟",
                    phoneticArabic = "وير دو يو ليف؟",
                    tipArabic = "Where للسؤال عن المكان."
                ),
                LessonPhrase(
                    english = "What does this word mean?",
                    arabic = "ماذا تعني هذه الكلمة؟",
                    phoneticArabic = "وات دَز ذِس وورد مين؟",
                    tipArabic = "عبارة ذهبية تسأل بها عن أي كلمة جديدة!"
                ),
                LessonPhrase(
                    english = "Could you speak slowly?",
                    arabic = "هل يمكنك التحدث ببطء؟",
                    phoneticArabic = "كود يو سبيك سلولي؟",
                    tipArabic = "مفيدة جداً أثناء التحدث مع الناطقين بالإنجليزية."
                ),
                LessonPhrase(
                    english = "When does the bus arrive?",
                    arabic = "متى تصل الحافلة؟",
                    phoneticArabic = "وين دَز ذَ باس أرايف؟",
                    tipArabic = "When للسؤال عن الوقت."
                )
            )
        )
    )
}
