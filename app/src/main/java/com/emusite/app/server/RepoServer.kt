package com.emusite.app.server

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.URLDecoder
import kotlin.concurrent.thread

class RepoServer(
    private val port: Int,
    private val onRepoReceived: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var running = false

    fun start() {
        running = true
        thread {
            try {
                serverSocket = ServerSocket(port)
                while (running) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
    }

    private fun handleClient(client: java.net.Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val output = client.getOutputStream()

            val requestLine = reader.readLine() ?: return

            if (requestLine.startsWith("POST")) {
                var contentLength = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("Content-Length:")) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    if (line.isEmpty()) break
                }

                val body = CharArray(contentLength)
                reader.read(body, 0, contentLength)
                val postData = String(body)
                val repoUrl = postData.substringAfter("url=").let {
                    URLDecoder.decode(it, "UTF-8")
                }

                if (repoUrl.isNotBlank()) {
                    onRepoReceived(repoUrl)
                }

                sendHtml(output, "<h1>Repository added!</h1><p>You can close this page.</p>")
            } else {
                sendHtml(
                    output, """
<!DOCTYPE html>
<html>
<head><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Emusite - Add Repository</title>
<style>
body{font-family:sans-serif;background:#0f0f1a;color:#fff;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0}
div{background:#1a1a2e;padding:30px;border-radius:12px;width:90%;max-width:400px}
input{width:100%;padding:12px;margin:10px 0;border:1px solid #333;border-radius:8px;background:#0f0f1a;color:#fff;font-size:16px}
button{width:100%;padding:14px;background:#e94560;color:#fff;border:none;border-radius:8px;font-size:16px;cursor:pointer}
h2{margin:0 0 8px;font-size:20px}
p{color:#888;font-size:14px;margin:0 0 20px}
.success{color:#4caf50;margin-top:10px;display:none}
</style>
</head>
<body>
<div>
<h2>Emusite</h2>
<p>Paste the repository URL to add it to your Firestick</p>
<form method="POST">
<input type="text" name="url" placeholder="emusiterepo://..." autofocus>
<button type="submit">Add Repository</button>
</form>
</div>
</body>
</html>
""".trimIndent()
                )
            }

            client.close()
        } catch (_: Exception) {}
    }

    private fun sendHtml(output: OutputStream, html: String) {
        val response = """
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8
Content-Length: ${html.toByteArray().size}
Connection: close

$html
""".trimIndent()
        output.write(response.toByteArray())
        output.flush()
    }
}
