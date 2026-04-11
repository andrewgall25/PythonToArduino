package com.andrea.pythontoarduino.server // Cambia con il tuo package effettivo

import android.content.Context
import android.util.Log // Importa Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.io.InputStream

class AssetServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    init {
        // Aggiungi un log quando il server viene istanziato
        Log.d("AssetServer", "AssetServer istanziato sulla porta: $port")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d("AssetServer", "Richiesta ricevuta per URI: $uri")

        var mimeType = "text/html" // Default MIME type

        // Determina il MIME type in base all'estensione del file
        if (uri.endsWith(".html")) mimeType = "text/html"
        else if (uri.endsWith(".css")) mimeType = "text/css"
        else if (uri.endsWith(".js")) mimeType = "application/javascript"
        else if (uri.endsWith(".json")) mimeType = "application/json"
        else if (uri.endsWith(".ttf")) mimeType = "font/ttf" // Per i font di Monaco
        else if (uri.endsWith(".woff")) mimeType = "font/woff"
        else if (uri.endsWith(".woff2")) mimeType = "font/woff2"
        else if (uri.endsWith(".svg")) mimeType = "image/svg+xml" // Aggiunto per SVG, se Monaco li usa
        else if (uri.endsWith(".eot")) mimeType = "application/vnd.ms-fontobject" // Aggiunto per EOT
        else if (uri.endsWith(".otf")) mimeType = "font/otf" // Aggiunto per OTF

        // Rimuovi lo slash iniziale per accedere correttamente alla risorsa asset
        val assetPath = if (uri.startsWith("/")) uri.substring(1) else uri
        Log.d("AssetServer", "MIME Type determinato: $mimeType, Asset Path: $assetPath")

        try {
            val inputStream: InputStream = context.assets.open(assetPath)
            val response = newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, inputStream.available().toLong())
            Log.d("AssetServer", "File '$assetPath' servito con successo.")
            return response
        } catch (ioe: IOException) {
            Log.e("AssetServer", "IOException durante il servizio del file '$assetPath': ${ioe.message}")
            // Se il file non viene trovato, prova a servire l'index.html o un 404
            if (assetPath == "" || assetPath == "index.html") { // Richiesta per la root o index.html esplicito
                try {
                    val indexStream: InputStream = context.assets.open("editor.html") // Assumiamo editor.html è il tuo index
                    val response = newFixedLengthResponse(Response.Status.OK, "text/html", indexStream, indexStream.available().toLong())
                    Log.d("AssetServer", "Servito 'editor.html' come fallback per la root/index.")
                    return response
                } catch (e: IOException) {
                    Log.e("AssetServer", "Errore: 'editor.html' non trovato in assets come fallback: ${e.message}")
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error: editor.html not found in assets.")
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error: File not found in assets: $assetPath")
        } catch (e: Exception) { // Cattura qualsiasi altra eccezione inaspettata
            Log.e("AssetServer", "Errore inatteso durante il servizio del file '$assetPath': ${e.message}", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Server Error: ${e.message}")
        }
    }
}
