package noobroutes.config

import com.google.gson.*
import noobroutes.Core.logger
import noobroutes.Core.mc
import java.io.File
import java.io.IOException

object DataManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun saveDataToFile(fileName: String, dataList: JsonArray) {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        try {
            path.parentFile?.mkdirs() ?: throw IOException("Failed to create directories")
            if (!path.exists()) {
                path.createNewFile()
            }

            path.bufferedWriter().use {
                val jsonArray = JsonArray().apply { dataList.forEach { jsonObject -> add(jsonObject) } }
                it.write(gson.toJson(jsonArray))
            }
        } catch (e: IOException) {
            logger.info("Error saving to ${path.path}, Array")
            e.printStackTrace()
        }
    }

    fun saveDataToFile(fileName: String, dataList: JsonObject) {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        try {
            path.parentFile?.mkdirs() ?: throw IOException("Failed to create directories")
            if (!path.exists()) {
                path.createNewFile()
            }
            val gson = GsonBuilder().setPrettyPrinting().create()
            path.bufferedWriter().use {
                it.write(gson.toJson(dataList))
            }
        } catch (e: IOException) {
            logger.info("Error saving to ${path.path}, Object")
            e.printStackTrace()
        }
    }

    fun loadDataFromFileObject(fileName: String): Map<String, JsonArray> {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        return try {
            path.bufferedReader().use { reader ->
                val jsonContent = reader.readText()
                val gson: Gson = GsonBuilder().setPrettyPrinting().create()
                val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
                val jsonArrays = mutableMapOf<String, JsonArray>()
                for ((key, value) in jsonObject.entrySet()) {
                    if (value.isJsonArray) {
                        jsonArrays[key] = value.asJsonArray
                    }
                }
                jsonArrays
            }
        } catch (e: java.nio.file.NoSuchFileException) {
            logger.info("File not found: ${path.path}")
            mutableMapOf()
        } catch (e: IOException) {
            e.printStackTrace()
            mutableMapOf()
        } catch (e: JsonSyntaxException) {
            logger.info("Invalid JSON syntax in file: ${path.path}")
            mutableMapOf()
        } catch (e: Exception) {
            logger.info("Error loading data from file: ${path.path}")
            e.printStackTrace()
            mutableMapOf()
        }
    }

    fun fileType(fileName: String): String? {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        return try {
            path.bufferedReader().use { reader ->
                val jsonElement = JsonParser().parse(reader)
                when {
                    jsonElement.isJsonArray -> "JsonArray"
                    jsonElement.isJsonNull -> "JsonNull"
                    jsonElement.isJsonObject -> "JsonObject"
                    jsonElement.isJsonPrimitive -> "JsonPrimitive"
                    else -> null
                }
            }
        } catch (e: java.nio.file.NoSuchFileException) {
            logger.info("File not found: ${path.path}")
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: JsonSyntaxException) {
            logger.info("Invalid JSON syntax in file: ${path.path}")
            null
        } catch (e: Exception) {
            logger.info("Error loading data from file: ${path.path}")
            e.printStackTrace()
            null
        }
    }

    fun loadDataFromFileArray(fileName: String): List<JsonObject> {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        return try {
            path.bufferedReader().use { reader ->
                val jsonContent = reader.readText()
                val jsonArray = gson.fromJson(jsonContent, JsonArray::class.java)
                jsonArray.map { it.asJsonObject }
            }
        } catch (e: java.nio.file.NoSuchFileException) {
            logger.info("File not found: ${path.path}")
            emptyList()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        } catch (e: JsonSyntaxException) {
            logger.info("Invalid JSON syntax in file: ${path.path}")
            emptyList()
        } catch (e: Exception) {
            logger.info("Error loading data from file: ${path.path}")
            e.printStackTrace()
            emptyList()
        }
    }
}