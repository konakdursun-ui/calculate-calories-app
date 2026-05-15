# Calorie AI Server

Android uygulaması API anahtarını taşımamalı. Bu backend fotoğrafı alır, Gemini veya OpenAI ile analiz eder ve uygulamaya kalori/makro JSON'u döndürür.

Varsayılan sağlayıcı Gemini'dir, çünkü free tier ile başlamak daha ucuzdur.

## Gemini API Key

1. Google AI Studio'ya gir.
2. API key oluştur.
3. Backend ortamına `GEMINI_API_KEY` olarak ekle.

## Local Çalıştırma

```powershell
cd backend\calorie-ai-server
$env:AI_PROVIDER="gemini"
$env:GEMINI_API_KEY="AIza..."
$env:GEMINI_MODEL="gemini-2.5-flash-lite"
npm start
```

Sunucu varsayılan olarak `http://0.0.0.0:8787` adresinde çalışır.

Test:

```powershell
curl http://localhost:8787/health
```

Beklenen cevap:

```json
{"ok":true,"provider":"gemini","model":"gemini-2.5-flash-lite"}
```

## Render Environment Variables

Render'da Web Service veya Blueprint oluştururken şunları ekle:

```text
AI_PROVIDER=gemini
GEMINI_API_KEY=AIza...
GEMINI_MODEL=gemini-2.5-flash-lite
```

## Android Endpoint Ayarı

Emulator için varsayılan endpoint:

```text
http://10.0.2.2:8787/analyze-food
```

Render deploy sonrası gerçek telefon/build için:

```powershell
.\gradlew.bat assembleCalculatecaloriesDebug -PCALORIE_AI_ENDPOINT="https://SENIN-RENDER-URL.onrender.com/analyze-food"
```

## OpenAI'ye Sonradan Geçiş

İleride OpenAI kullanmak istersen Render env değerlerini şöyle değiştir:

```text
AI_PROVIDER=openai
OPENAI_API_KEY=sk-proj-...
OPENAI_MODEL=gpt-4.1-mini
```
