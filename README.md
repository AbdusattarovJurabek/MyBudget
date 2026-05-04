# My Budget Android App

Kotlin + Jetpack Compose + Firebase Auth + Cloud Firestore asosidagi kirim/chiqim ilova skeleti.

## Firebase sozlash

1. Firebase Console oching.
2. New project yarating: `My Budget`.
3. Android app qo‘shing:
   - Package name: `uz.mybudget.app`
4. `google-services.json` faylini yuklab oling.
5. Faylni quyidagi joyga qo‘ying:

```text
MyBudgetApp/app/google-services.json
```

6. Firebase Authentication → Sign-in method → Email/Password → Enable.
7. Firestore Database → Create database → Production mode.
8. Firestore Rules quyidagicha bo‘lsin:

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Firestore structure

```text
users/{userId}/transactions/{transactionId}
```

## Transaction model

- id
- type: income/expense
- amount
- category
- note
- date
- createdAt
- updatedAt

## Ishga tushirish

1. Android Studio’da `MyBudgetApp` papkasini oching.
2. Gradle Sync qiling.
3. `google-services.json` qo‘ying.
4. Emulator yoki telefonda Run qiling.

## Eslatma

Bu loyiha boshlang‘ich professional skelet. Keyingi bosqichda quyidagilar qo‘shiladi:
- Bottom Navigation
- DatePicker
- Statistik grafiklar
- Tahrirlash/o‘chirish oynasi
- Room Database bilan lokal cache
- Hilt dependency injection
