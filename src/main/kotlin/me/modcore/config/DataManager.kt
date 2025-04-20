package me.modcore.config

import com.google.gson.*
import me.modcore.Core.mc
import java.io.File
import java.io.IOException

object DataManager {
    fun saveDataToFile(fileName: String, dataList: JsonArray) {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        try {
            path.parentFile?.mkdirs() ?: throw IOException("Failed to create directories")


            if (!path.exists()) {
                path.createNewFile()
            }
            val gson = GsonBuilder().setPrettyPrinting().create()
            path.bufferedWriter().use {
                val jsonArray = JsonArray().apply { dataList.forEach { jsonObject -> add(jsonObject) } }
                it.write(gson.toJson(jsonArray))
            }
        } catch (e: IOException) {
            println("Error saving to ${path.path}")
            e.printStackTrace()
        }
    }

    fun loadDataFromFile(fileName: String): List<JsonObject> {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        return try {
            path.bufferedReader().use { reader ->
                val jsonContent = reader.readText()
                val gson: Gson = GsonBuilder().setPrettyPrinting().create()
                val jsonArray = gson.fromJson(jsonContent, JsonArray::class.java)
                jsonArray.map { it.asJsonObject }
            }
        } catch (e: java.nio.file.NoSuchFileException) {
            println("File not found: ${path.path}")
            emptyList()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        } catch (e: JsonSyntaxException) {
            println("Invalid JSON syntax in file: ${path.path}")
            emptyList()
        } catch (e: Exception) {
            println("Error loading data from file: ${path.path}")
            e.printStackTrace()
            emptyList()
        }
    }
}