# My Budget Android App

Kotlin va Jetpack Compose asosidagi lokal kirim-chiqim ilovasi.

## Asosiy imkoniyatlar

- Email va parolsiz, 4–6 raqamli PIN bilan kirish.
- PIN ochiq holda saqlanmaydi; PBKDF2 xeshi va tasodifiy salt ishlatiladi.
- Operatsiyalar telefonning lokal SQLite bazasida saqlanadi.
- Barcha ma’lumotlarni haqiqiy `.xlsx` Excel fayliga eksport qilish.
- `.xlsx` yoki `.csv` fayldan eski ma’lumotlarni qayta import qilish.
- Android fayl tanlagichi orqali eksport papkasi va import faylini erkin tanlash.

## Excel ustunlari

`ID`, `Turi`, `Summa`, `Kategoriya`, `Izoh`, `Sana`, `Yaratilgan vaqt`, `Yangilangan vaqt`.

`Turi` ustunida `income` yoki `expense` ishlatiladi. Import paytida `kirim`, `daromad` va `income` qiymatlari kirim sifatida qabul qilinadi.

## Ishga tushirish

1. Loyihani Android Studio’da oching.
2. Gradle Sync bajaring.
3. Emulator yoki Android telefonda Run bosing.
4. Birinchi ishga tushishda PIN kod yarating.

## Ma’lumotlar ko‘chishi

Oldingi ma’lumotlarni ilovadagi **Sozlamalar → Fayldan tiklash** orqali import qiling. Bir xil ID bilan qayta import qilingan qator yangilanadi, yangi ID esa yangi operatsiya sifatida qo‘shiladi.
