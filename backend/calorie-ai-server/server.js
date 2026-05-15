import http from "node:http";

const PORT = Number(process.env.PORT || 8787);
const MODEL = process.env.OPENAI_MODEL || "gpt-5.2";
const MAX_BODY_BYTES = 8 * 1024 * 1024;

const foodSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    food_name: { type: "string" },
    confidence: { type: "number", minimum: 0, maximum: 1 },
    portion_grams: { type: "integer", minimum: 30, maximum: 1500 },
    calories: { type: "integer", minimum: 0 },
    protein_g: { type: "number", minimum: 0 },
    carbs_g: { type: "number", minimum: 0 },
    fat_g: { type: "number", minimum: 0 },
    notes: { type: "string" }
  },
  required: [
    "food_name",
    "confidence",
    "portion_grams",
    "calories",
    "protein_g",
    "carbs_g",
    "fat_g",
    "notes"
  ]
};

const server = http.createServer(async (req, res) => {
  setCorsHeaders(res);

  if (req.method === "OPTIONS") {
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.method === "GET" && req.url === "/health") {
    sendJson(res, 200, { ok: true, model: MODEL });
    return;
  }

  if (req.method !== "POST" || req.url !== "/analyze-food") {
    sendJson(res, 404, { error: "Not found" });
    return;
  }

  try {
    if (!process.env.OPENAI_API_KEY) {
      sendJson(res, 500, { error: "OPENAI_API_KEY is not configured on the server." });
      return;
    }

    const body = await readJsonBody(req);
    const imageBase64 = String(body.imageBase64 || "");
    if (!imageBase64) {
      sendJson(res, 400, { error: "imageBase64 is required." });
      return;
    }

    const analysis = await analyzeFood(imageBase64);
    sendJson(res, 200, analysis);
  } catch (error) {
    sendJson(res, 500, {
      error: "Food analysis failed.",
      detail: error instanceof Error ? error.message : String(error)
    });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`Calorie AI server listening on http://0.0.0.0:${PORT}`);
});

async function analyzeFood(imageBase64) {
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: MODEL,
      input: [
        {
          role: "user",
          content: [
            {
              type: "input_text",
              text: [
                "Fotoğraftaki yemeği analiz et ve sadece şemaya uygun JSON üret.",
                "Türkiye'de yaygın yemekleri özellikle düşün: baklava, lahmacun, pide, döner, kebap, pilav, makarna, börek, poğaça, salata, çorba, tatlı.",
                "Yemeğin adını mümkün olduğunca spesifik yaz. Emin değilsen en yakın kategoriyle birlikte belirt.",
                "Porsiyonu fotoğrafa göre tahmin et. Kalori ve makroları seçtiğin porsiyonun toplam değeri olarak hesapla.",
                "Beslenme değerleri yaklaşık olmalı; notes alanında kısa Türkçe açıklama yaz."
              ].join(" ")
            },
            {
              type: "input_image",
              image_url: `data:image/jpeg;base64,${imageBase64}`
            }
          ]
        }
      ],
      text: {
        format: {
          type: "json_schema",
          name: "food_analysis",
          strict: true,
          schema: foodSchema
        }
      }
    })
  });

  const responseText = await response.text();
  if (!response.ok) {
    throw new Error(responseText);
  }

  const payload = JSON.parse(responseText);
  const outputText = extractOutputText(payload);
  if (!outputText) {
    throw new Error("OpenAI response did not include output text.");
  }

  return normalizeAnalysis(JSON.parse(outputText));
}

function normalizeAnalysis(value) {
  return {
    food_name: String(value.food_name || "Yemek analizi"),
    confidence: clampNumber(Number(value.confidence || 0.7), 0, 1),
    portion_grams: clampInteger(Number(value.portion_grams || 250), 30, 1500),
    calories: Math.max(0, Math.round(Number(value.calories || 0))),
    protein_g: roundOne(Math.max(0, Number(value.protein_g || 0))),
    carbs_g: roundOne(Math.max(0, Number(value.carbs_g || 0))),
    fat_g: roundOne(Math.max(0, Number(value.fat_g || 0))),
    notes: String(value.notes || "AI ile tahmini besin analizi yapıldı.")
  };
}

function extractOutputText(payload) {
  if (typeof payload.output_text === "string") {
    return payload.output_text;
  }
  for (const item of payload.output || []) {
    for (const content of item.content || []) {
      if (typeof content.text === "string") {
        return content.text;
      }
    }
  }
  return "";
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let total = 0;
    const chunks = [];

    req.on("data", chunk => {
      total += chunk.length;
      if (total > MAX_BODY_BYTES) {
        reject(new Error("Request body is too large."));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });

    req.on("end", () => {
      try {
        const raw = Buffer.concat(chunks).toString("utf8");
        resolve(raw ? JSON.parse(raw) : {});
      } catch (error) {
        reject(error);
      }
    });

    req.on("error", reject);
  });
}

function sendJson(res, status, payload) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(payload));
}

function setCorsHeaders(res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization");
}

function clampNumber(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function clampInteger(value, min, max) {
  return Math.max(min, Math.min(max, Math.round(value)));
}

function roundOne(value) {
  return Math.round(value * 10) / 10;
}
