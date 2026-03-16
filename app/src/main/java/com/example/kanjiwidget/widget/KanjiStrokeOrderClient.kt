package com.example.kanjiwidget.widget

import java.net.HttpURLConnection
import java.net.URL

object KanjiStrokeOrderClient {
    private const val BASE_URL = "https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji"
    private val viewBoxRegex =
        Regex("""viewBox\s*=\s*"([\-0-9.]+)\s+([\-0-9.]+)\s+([\-0-9.]+)\s+([\-0-9.]+)"""")

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

    fun buildAnimatedHtml(
        svg: String,
        kanji: String,
        strokeColorHex: String,
        strokeNumberColorHex: String,
    ): String {
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
                  color: $strokeColorHex;
                  font-family: sans-serif;
                  overflow: hidden;
                }
                body {
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  min-height: 100vh;
                  padding: 10px;
                }
                #stroke-root {
                  width: min(88vw, 430px);
                  height: min(88vw, 430px);
                  display: flex;
                  justify-content: center;
                  align-items: center;
                }
                svg {
                  width: 100%;
                  height: 100%;
                  display: block;
                  overflow: visible;
                }
                #stroke-root path {
                  stroke: $strokeColorHex !important;
                  stroke-width: 4 !important;
                  fill: none !important;
                  stroke-linecap: round !important;
                  stroke-linejoin: round !important;
                }
                [id*="StrokeNumbers"] {
                  opacity: 0.9;
                  color: $strokeNumberColorHex !important;
                  fill: $strokeNumberColorHex !important;
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
        val svg = rawSvg.substring(svgStart).trim()
        return expandViewBox(svg)
    }

    private fun expandViewBox(svg: String): String {
        val match = viewBoxRegex.find(svg) ?: return svg
        val minX = match.groupValues[1].toDoubleOrNull() ?: return svg
        val minY = match.groupValues[2].toDoubleOrNull() ?: return svg
        val width = match.groupValues[3].toDoubleOrNull() ?: return svg
        val height = match.groupValues[4].toDoubleOrNull() ?: return svg
        val padding = maxOf(width, height) * 0.09
        val expandedViewBox = "viewBox=\"${formatDecimal(minX - padding)} ${formatDecimal(minY - padding)} ${formatDecimal(width + (padding * 2))} ${formatDecimal(height + (padding * 2))}\""
        return svg.replaceRange(match.range, expandedViewBox)
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            "%.2f".format(java.util.Locale.US, value)
        }
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
