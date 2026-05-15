# Igne Atma - Release Checklist

## Teknik Kontrol
- Debug APK derleniyor: `.\gradlew.bat assembleDebug`
- Telefon testi: USB hata ayiklama acik cihazla `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- Release icin Android Studio uzerinden signed AAB alinacak: Build > Generate Signed Bundle / APK > Android App Bundle
- `versionCode` ve `versionName` her yayin oncesi artirilacak.

## Reklam Kontrolu
- Telefon testinde sorun cikarmamasi icin Google Ads SDK su an devre disi.
- Oyundaki odullu reklam butonlari su an yerel odul simulatörü gibi calisir.
- Play Store'a gercek reklamla cikilacaksa AdMob SDK tekrar eklenecek ve `app/src/main/res/values/strings.xml` icindeki `admob_app_id` / `rewarded_ad_unit_id` gercek kimliklerle kullanilacak.
- Gercek reklam entegrasyonu yapildiktan sonra Play Console'da "reklam iceriyor" secenegi isaretlenecek.

## Play Store Listeleme
- Uygulama adi: Igne Atma
- Kisa aciklama ve uzun aciklama `docs/play-store-listing-tr.md` icinde hazir.
- Ingilizce liste metni `docs/play-store-listing-en.md` icinde hazir.
- Gizlilik politikasi taslagi `docs/privacy-policy-tr.md` icinde hazir.

## Gorsel Gereksinimler
- Uygulama ikonu eklendi.
- Splash screen eklendi.
- Play Store icin ayrica ekran goruntuleri alinmali.
- Feature graphic hazirlanmali: 1024x500 px.
- En az 2 telefon ekran goruntusu yuklenmeli.

## Play Console Formlari
- Veri guvenligi formu doldurulacak.
- Icerik derecelendirme anketi doldurulacak.
- Reklam iceriyor secenegi isaretlenecek.
- Hedef kitle ve icerik ayarlari yapilacak.
