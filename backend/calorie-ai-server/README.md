# Calorie AI Server

Android uygulaması API anahtarını taşımamalı. Bu küçük backend fotoğrafı alır, OpenAI Responses API ile analiz eder ve uygulamaya kalori/makro JSON'u döndürür.

## Çalıştırma

```powershell
cd backend\calorie-ai-server
$env:OPENAI_API_KEY="sk-proj-..."
npm start
```

Sunucu varsayılan olarak `http://0.0.0.0:8787` adresinde çalışır.

## Android endpoint ayarı

Emulator için varsayılan endpoint:

```text
http://10.0.2.2:8787/analyze-food
```

Gerçek telefon için bilgisayarının yerel IP adresini kullan:

```powershell
.\gradlew.bat assembleCalculatecaloriesDebug -PCALORIE_AI_ENDPOINT="http://BILGISAYAR_IP:8787/analyze-food"
```

Örnek:

```powershell
.\gradlew.bat assembleCalculatecaloriesDebug -PCALORIE_AI_ENDPOINT="http://192.168.1.34:8787/analyze-food"
```

Telefon ve bilgisayar aynı Wi-Fi ağında olmalı. Windows güvenlik duvarı portu sorarsa `8787` için izin ver.

## Test

```powershell
curl http://localhost:8787/health
```

Beklenen cevap:

```json
{"ok":true,"model":"gpt-5.2"}
```
