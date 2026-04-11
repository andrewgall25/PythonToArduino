package com.andrea.pythontoarduino

import android.content.Context
import android.util.Log
import java.io.File

object ScriptManager {

    private const val FOLDER_NAME = "PythonToArduino"

    private fun getScriptDirectory(context: Context): File {
        return File(context.getExternalFilesDir(null), FOLDER_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    fun saveScript(context: Context, fileName: String, content: String): Boolean {
        return try {
            val dir = getScriptDirectory(context)
            val file = File(dir, "$fileName.py")
            file.writeText(content)
            Log.d("ScriptManager", "Script salvato in: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("ScriptManager", "Errore salvataggio: ${e.message}")
            false
        }
    }

    fun loadScript(context: Context, fileName: String): String? {
        return try {
            val file = File(getScriptDirectory(context), "$fileName.py")
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            Log.e("ScriptManager", "Errore lettura: ${e.message}")
            null
        }
    }

    fun listScripts(context: Context): List<String> {
        val dir = getScriptDirectory(context)
        return dir.listFiles { f -> f.extension == "py" }
            ?.map { it.nameWithoutExtension } ?: emptyList()
    }

    fun deleteScript(context: Context, fileName: String): Boolean {
        return try {
            val file = File(getScriptDirectory(context), "$fileName.py")
            file.exists() && file.delete()
        } catch (e: Exception) {
            Log.e("ScriptManager", "Errore eliminazione: ${e.message}")
            false
        }
    }

    fun getScriptPath(context: Context, fileName: String): String {
        val file = File(getScriptDirectory(context), "$fileName.py")
        return file.absolutePath
    }
}
