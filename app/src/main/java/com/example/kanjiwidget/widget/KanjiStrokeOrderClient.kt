package com.example.kanjiwidget.widget

import java.net.HttpURLConnection
import java.net.URL

object KanjiStrokeOrderClient {
    private const val BASE_URL = "https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji"

    fun fetchSvg(kanji: String): String? {
        val normalized = kanji.trim()
        if (normalized.isEmpty()) return null

        val codePoint = normalized.codePointAt(0)
        val hex = codePoint.toString(16).padStart(5, '0')
        val url = URL("$BASE_URL/$hex.svg")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }

        return try {
            if (conn.responseCode !in 200..299) {
                null
            } else {
                conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    fun buildAnimatedHtml(svg: String, kanji: String): String {
        val cleanSvg = sanitizeSvg(svg)

        val escapedKanji = escapeHtml(kanji)

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <style>
                :root {
                  color-scheme: light;
                }
                html, body {
                  margin: 0;
                  padding: 0;
                  background: transparent;
                  color: #1f1f1f;
                  font-family: sans-serif;
                  overflow: hidden;
                }
                body {
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  min-height: 100vh;
                  padding: 0;
                }
                svg {
                  width: min(92vw, 460px);
                  height: min(92vw, 460px);
                  display: block;
                }
                #stroke-root path {
                  stroke: #171717 !important;
                  stroke-width: 4 !important;
                  fill: none !important;
                  stroke-linecap: round !important;
                  stroke-linejoin: round !important;
                }
                [id*="StrokeNumbers"] {
                  opacity: 0.9;
                }
              </style>
            </head>
            <body>
              <div id="stroke-root" aria-label="Stroke order for $escapedKanji">
                $cleanSvg
              </div>
              <script>
                function preparePaths() {
                  const paths = Array.from(document.querySelectorAll('#stroke-root g[id*="StrokePaths"] path'));
                  paths.forEach((path) => {
                    const length = path.getTotalLength();
                    path.style.strokeDasharray = length;
                    path.style.strokeDashoffset = length;
                    path.style.transition = 'none';
                  });
                  return paths;
                }

                function restartStrokeOrder() {
                  const paths = preparePaths();
                  let delay = 120;
                  paths.forEach((path) => {
                    setTimeout(() => {
                      path.style.transition = 'stroke-dashoffset 700ms ease';
                      path.style.strokeDashoffset = '0';
                    }, delay);
                    delay += 420;
                  });
                }

                window.restartStrokeOrder = restartStrokeOrder;
                window.addEventListener('load', function() {
                  restartStrokeOrder();
                });
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun sanitizeSvg(rawSvg: String): String {
        val svgStart = rawSvg.indexOf("<svg")
        if (svgStart < 0) return rawSvg.trim()
        return rawSvg.substring(svgStart).trim()
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
