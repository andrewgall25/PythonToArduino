package com.andrea.pythontoarduino

import java.io.OutputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import android.util.Log
import java.nio.charset.Charset

class PythonOutputStream(
    private val onOutput: (String) -> Unit
) : OutputStream() {

    // Coda bloccante per memorizzare i byte in uscita.
    private val queue = LinkedBlockingQueue<Int>()

    // Buffer sincronizzato per accumulare i byte
    private val byteBuffer = mutableListOf<Byte>() // Usiamo un buffer di byte per gestire la codifica

    // Flag per indicare se lo stream è stato chiuso.
    @Volatile
    private var closed = false

    // Segnale speciale per indicare un flush
    private val FLUSH_SIGNAL = -2 // Usiamo un valore che non sia un byte valido (-1 è EOF)

    init {
        Log.d("PythonOutputStream", "PythonOutputStream initialized.")
    }

    /**
     * Scrive il byte specificato nello stream.
     * Questo metodo è chiamato da Python.
     */
    override fun write(byte: Int) {
        synchronized(byteBuffer) {
            if (closed) {
                Log.w("PythonOutputStream", "write(byte): Attempt to write on closed stream.")
                return
            }
            try {
                queue.put(byte) // Aggiungi il byte alla coda
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e("PythonOutputStream", "write(byte): Writing interrupted", e)
            }
        }
    }

    /**
     * Scrive un array di byte nello stream.
     * Questo metodo è chiamato da Python quando scrive un blocco di byte.
     */
    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    /**
     * Scrive un sottoinsieme di un array di byte nello stream.
     * Questo metodo è chiamato da Python quando scrive un blocco di byte.
     */
    override fun write(b: ByteArray, off: Int, len: Int) {
        synchronized(byteBuffer) {
            if (closed) {
                Log.w("PythonOutputStream", "write(bytes, off, len): Attempt to write on closed stream.")
                return
            }
            for (i in off until off + len) {
                try {
                    queue.put(b[i].toInt() and 0xFF)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e("PythonOutputStream", "write(bytes): Writing interrupted", e)
                    return
                }
            }
        }
    }

    /**
     * Scrive una stringa nello stream.
     * Questo metodo è quello che Python si aspetta per sys.stdout.write().
     */
    fun write(text: String) {
        Log.d("PythonOutputStream", "write(String): Writing '$text'")
        write(text.toByteArray(Charsets.UTF_8))
    }

    /**
     * Svuota tutti i byte attualmente nel buffer e li invia tramite il callback.
     * Questo è fondamentale per i prompt di input che non terminano con un newline.
     * Ora invia un segnale di flush alla coda.
     */
    override fun flush() {
        synchronized(byteBuffer) {
            if (closed) {
                Log.w("PythonOutputStream", "flush(): Attempt to flush on closed stream.")
                return
            }
            try {
                queue.put(FLUSH_SIGNAL) // Invia il segnale di flush
                Log.d("PythonOutputStream", "flush(): Called flush. Sending FLUSH_SIGNAL signal.")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e("PythonOutputStream", "flush(): Interrupted while sending flush signal", e)
            }
        }
    }

    /**
     * Legge i byte dalla coda e li converte in stringhe, inviandoli tramite il callback.
     * Questo metodo è eseguito in un thread separato.
     */
    // Aggiungi un'etichetta al ciclo while, chiamiamola 'loop@'
    fun readAndProcessOutput() {
        Log.d("PythonOutputStream", "readAndProcessOutput(): Starting reading output.")

        // **MODIFICA QUI**
        loop@ while (!closed || queue.isNotEmpty()) {
            try {
                val byteValue = queue.poll(100, TimeUnit.MILLISECONDS)
                if (byteValue != null) {
                    when (byteValue) {
                        FLUSH_SIGNAL -> {
                            Log.d("PythonOutputStream", "readAndProcessOutput(): Ricevuto FLUSH_SIGNAL. Processo buffer.")
                            processBufferedBytes(levelFlush = true)
                        }
                        -1 -> {
                            Log.d("PythonOutputStream", "readAndProcessOutput(): Ricevuto segnale di fine stream (-1).")
                            processBufferedBytes(levelFlush = true)
                            break@loop // **MODIFICA QUI: Aggiunto l'etichetta**
                        }
                        else -> {
                            synchronized(byteBuffer) {
                                byteBuffer.add(byteValue.toByte())
                                if (byteValue.toChar() == '\n' || byteBuffer.size >= 1024) {
                                    processBufferedBytes(levelFlush = true)
                                }
                            }
                        }
                    }
                } else if (closed && queue.isEmpty()) {
                    Log.d("PythonOutputStream", "readAndProcessOutput(): Stream chiuso e coda vuota.")
                    processBufferedBytes(levelFlush = true)
                    break@loop // **MODIFICA QUI: Aggiunto l'etichetta**
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e("PythonOutputStream", "readAndProcessOutput(): Lettura interrotta", e)
                break@loop // **MODIFICA QUI: Aggiunto l'etichetta**
            } catch (e: Exception) {
                Log.e("PythonOutputStream", "readAndProcessOutput(): Errore durante la lettura dell'output: ${e.message}", e)
                onOutput("Errore lettura output: ${e.message}\n")
                break@loop // **MODIFICA QUI: Aggiunto l'etichetta**
            }
        }

        Log.d("PythonOutputStream", "readAndProcessOutput(): Loop di lettura terminato. Processo byte residui.")
        processBufferedBytes(levelFlush = true)
    }

    /**
     * Processa i byte accumulati nel buffer e li invia tramite il callback.
     */
    private fun processBufferedBytes(levelFlush: Boolean = false) {
        synchronized(byteBuffer) {
            if (byteBuffer.isNotEmpty()) {
                val bytes = byteBuffer.toByteArray()
                try {
                    val outputString = String(bytes, Charsets.UTF_8) // Converti usando UTF-8
                    if (outputString.isNotEmpty()) {
                        onOutput(outputString)
                        Log.d("PythonOutputStream", "processBufferedBytes(): Inviato output: '$outputString'")
                    } else {
                        Log.d("PythonOutputStream", "processBufferedBytes(): Buffer vuoto dopo decodifica.")
                    }
                } catch (e: Exception) {
                    Log.e("PythonOutputStream", "processBufferedBytes(): Errore di decodifica UTF-8: ${e.message}", e)
                    onOutput("Errore di decodifica: ${e.message}\n") // Invia un messaggio di errore
                } finally {
                    byteBuffer.clear()
                }
            } else if (levelFlush) {
                Log.d("PythonOutputStream", "processBufferedBytes(): Buffer vuoto su levelFlush, nessun output inviato.")
            } else {
                Log.d("PythonOutputStream", "processBufferedBytes(): Buffer vuoto, nessun output inviato.")
            }
        }
    }

    override fun close() {
        synchronized(byteBuffer) {
            if (!closed) {
                closed = true
                Log.d("PythonOutputStream", "close(): Stream chiuso.")
                processBufferedBytes(levelFlush = true)
                try {
                    queue.put(-1) // Segnale di fine stream per sbloccare i lettori
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            } else {
                Log.d("PythonOutputStream", "close(): Stream già chiuso.")
            }
        }
    }

    /**
     * Resetta lo stream, svuotando la coda e riaprendo lo stream.
     */
    fun reset() {
        synchronized(byteBuffer) {
            queue.clear()
            byteBuffer.clear() // Svuota anche il buffer dei byte
            closed = false
            Log.d("PythonOutputStream", "reset(): Stream resettato.")
        }
    }

    // --- METODI RICHIESTI DA io.TextIOWrapper ---

    /**
     * Metodo richiesto da Python per verificare se lo stream è chiuso.
     */
    fun closed(): Boolean {
        return closed
    }

    /**
     * Indica se lo stream è leggibile. Per un OutputStream, è false.
     */
    fun readable(): Boolean {
        return false
    }

    /**
     * Indica se lo stream è scrivibile. Per un OutputStream, è true.
     */
    fun writable(): Boolean {
        return true
    }

    /**
     * Indica se lo stream è seekable (supporta seek/tell). Per la nostra coda, è false.
     */
    fun seekable(): Boolean {
        return false
    }
}