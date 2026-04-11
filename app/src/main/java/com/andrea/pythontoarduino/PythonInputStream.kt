package com.andrea.pythontoarduino

import java.io.InputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import android.util.Log
import java.nio.charset.Charset

class PythonInputStream : InputStream() {
    private val queue = LinkedBlockingQueue<Int>()
    @Volatile
    private var closed = false

    init {
        Log.d("PythonInputStream", "PythonInputStream inizializzato.")
    }

    override fun read(): Int {
        synchronized(this) {
            if (closed && queue.isEmpty()) {
                Log.d("PythonInputStream", "read(): Stream chiuso e coda vuota, restituisco -1.")
                return -1
            }
            try {
                val byteValue = queue.poll(100, TimeUnit.MILLISECONDS)
                return byteValue ?: -1
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.w("PythonInputStream", "read(): Lettura interrotta, restituisco -1", e)
                return -1
            }
        }
    }

    fun write(bytes: ByteArray) {
        synchronized(this) {
            if (closed) {
                Log.w("PythonInputStream", "write(bytes): Tentativo di scrivere su stream chiuso.")
                return
            }
            for (byte in bytes) {
                try {
                    queue.put(byte.toInt() and 0xFF)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e("PythonInputStream", "write(bytes): Scrittura interrotta: ${e.message}")
                    return
                }
            }
            Log.d("PythonInputStream", "write(bytes): ${bytes.size} byte scritti. Coda size: ${queue.size}")
        }
    }

    fun write(text: String) {
        Log.d("PythonInputStream", "write(String): Scrivendo '$text'")
        write(text.toByteArray(Charsets.UTF_8))
    }

    fun readline(): String {
        synchronized(this) {
            val lineBytes = mutableListOf<Byte>()
            Log.d("PythonInputStream", "readline(): Inizio lettura riga.")
            while (!closed || queue.isNotEmpty()) {
                try {
                    val byteValue = queue.poll(100, TimeUnit.MILLISECONDS)
                    if (byteValue != null) {
                        if (byteValue == -1) {
                            Log.d("PythonInputStream", "readline(): Ricevuto segnale di fine stream (-1).")
                            break
                        }
                        val byte = byteValue.toByte()
                        lineBytes.add(byte)
                        if (byte.toChar() == '\n') {
                            Log.d("PythonInputStream", "readline(): Trovato newline.")
                            break
                        }
                    } else if (closed && queue.isEmpty()) {
                        Log.d("PythonInputStream", "readline(): Stream chiuso e coda vuota.")
                        break
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e("PythonInputStream", "readline(): Lettura riga interrotta", e)
                    break
                } catch (e: Exception) {
                    Log.e("PythonInputStream", "readline(): Errore durante la lettura della riga: ${e.message}", e)
                    break
                }
            }
            val result = String(lineBytes.toByteArray(), Charsets.UTF_8)
            Log.d("PythonInputStream", "readline(): Riga letta: '$result'")
            return result
        }
    }

    override fun close() {
        synchronized(this) {
            if (!closed) {
                closed = true
                Log.d("PythonInputStream", "close(): Stream chiuso.")
                try {
                    queue.put(-1)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            } else {
                Log.d("PythonInputStream", "close(): Stream già chiuso.")
            }
        }
    }

    fun resetStream() {
        synchronized(this) {
            queue.clear()
            closed = false
            Log.d("PythonInputStream", "resetStream(): Stream resettato.")
        }
    }

    fun closed(): Boolean {
        return closed
    }

    fun readable(): Boolean {
        return true
    }

    fun writable(): Boolean {
        return false
    }

    fun seekable(): Boolean {
        return false
    }
}