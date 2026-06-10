package com.yourname.habitapp.utils

data class TaskTemplate(val title: String, val icon: String)
data class TaskCategory(val name: String, val icon: String, val tasks: List<TaskTemplate>)

object TaskTemplates {

    val ALL_CATEGORIES = listOf(
        TaskCategory("الصحة واللياقة", "💪", listOf(
            TaskTemplate("شرب 8 أكواب ماء", "💧"),
            TaskTemplate("المشي 30 دقيقة", "🚶"),
            TaskTemplate("ممارسة الرياضة", "🏋️"),
            TaskTemplate("قياس الوزن", "⚖️"),
            TaskTemplate("تناول الفيتامينات", "💊"),
            TaskTemplate("النوم مبكراً", "😴"),
            TaskTemplate("تمارين التمدد", "🧘"),
            TaskTemplate("قياس ضغط الدم", "🩺")
        )),
        TaskCategory("التعلم والتطوير", "📚", listOf(
            TaskTemplate("قراءة كتاب", "📖"),
            TaskTemplate("دراسة لغة جديدة", "🗣️"),
            TaskTemplate("مشاهدة كورس", "🎓"),
            TaskTemplate("حل تمارين رياضية", "📐"),
            TaskTemplate("قراءة مقالات علمية", "🔬"),
            TaskTemplate("الاستماع لبودكاست", "🎙️"),
            TaskTemplate("ممارسة البرمجة", "💻"),
            TaskTemplate("مراجعة الملاحظات", "📝")
        )),
        TaskCategory("العمل والإنتاجية", "💼", listOf(
            TaskTemplate("مراجعة المهام اليومية", "✅"),
            TaskTemplate("الرد على الإيميلات", "📧"),
            TaskTemplate("اجتماع الفريق", "👥"),
            TaskTemplate("إعداد التقرير الأسبوعي", "📊"),
            TaskTemplate("تحديث المشاريع", "🔄"),
            TaskTemplate("التخطيط للغد", "📅"),
            TaskTemplate("مراجعة الأهداف", "🎯"),
            TaskTemplate("تنظيم سطح المكتب", "🖥️")
        )),
        TaskCategory("المنزل والعائلة", "🏠", listOf(
            TaskTemplate("ترتيب الغرفة", "🧹"),
            TaskTemplate("غسيل الملابس", "👕"),
            TaskTemplate("طبخ وجبة منزلية", "🍳"),
            TaskTemplate("التسوق الأسبوعي", "🛒"),
            TaskTemplate("قضاء وقت مع العائلة", "👨‍👩‍👧"),
            TaskTemplate("تنظيف المنزل", "🧺"),
            TaskTemplate("دفع الفواتير", "💳"),
            TaskTemplate("صيانة المنزل", "🔧")
        )),
        TaskCategory("الصحة النفسية", "🧘", listOf(
            TaskTemplate("التأمل 10 دقائق", "🧘"),
            TaskTemplate("كتابة اليوميات", "📔"),
            TaskTemplate("التنفس العميق", "🌬️"),
            TaskTemplate("الامتنان اليومي", "🙏"),
            TaskTemplate("قطع عن السوشيال ميديا", "📵"),
            TaskTemplate("الاسترخاء", "🛁"),
            TaskTemplate("الحديث مع صديق", "💬"),
            TaskTemplate("مشاهدة غروب الشمس", "🌅")
        )),
        TaskCategory("المال والادخار", "💰", listOf(
            TaskTemplate("تسجيل المصروفات", "📒"),
            TaskTemplate("مراجعة الميزانية", "📊"),
            TaskTemplate("تحويل للادخار", "🏦"),
            TaskTemplate("مراجعة الاستثمارات", "📈"),
            TaskTemplate("البحث عن صفقات", "🔍"),
            TaskTemplate("مراجعة الاشتراكات", "💳"),
            TaskTemplate("حساب التوفير", "🧮"),
            TaskTemplate("قراءة مقال مالي", "💡")
        )),
        TaskCategory("الديني والروحاني", "🕌", listOf(
            TaskTemplate("صلاة الفجر", "🌙"),
            TaskTemplate("قراءة القرآن", "📿"),
            TaskTemplate("الأذكار الصباحية", "☀️"),
            TaskTemplate("الأذكار المسائية", "🌙"),
            TaskTemplate("صيام نافلة", "🌙"),
            TaskTemplate("صدقة يومية", "❤️"),
            TaskTemplate("الاستغفار", "🤲"),
            TaskTemplate("حلقة علم", "📚"),
            TaskTemplate("زيارة الأهل", "🏡"),
            TaskTemplate("الدعاء", "🙏")
        )),
        TaskCategory("التقنية والتطوير", "💻", listOf(
            TaskTemplate("تطوير مشروع", "🛠️"),
            TaskTemplate("مراجعة الكود", "🔍"),
            TaskTemplate("كتابة اختبارات", "✅"),
            TaskTemplate("تعلم تقنية جديدة", "🚀"),
            TaskTemplate("متابعة GitHub", "🐙"),
            TaskTemplate("قراءة توثيق", "📄"),
            TaskTemplate("حل مشكلة برمجية", "🐛"),
            TaskTemplate("نشر تحديث", "🚀"),
            TaskTemplate("مراجعة الأمان", "🔒"),
            TaskTemplate("تحسين الأداء", "⚡")
        ))
    )

    fun getAll(): List<TaskTemplate> = ALL_CATEGORIES.flatMap { it.tasks }
}
