package noobroutes.config

import com.google.gson.*
import noobroutes.Core.logger
import noobroutes.Core.mc
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



object DataManager {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

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
            path.bufferedWriter().use {
                it.write(gson.toJson(dataList))
            }
        } catch (e: IOException) {
            logger.info("Error saving to ${path.path}, Object")
            e.printStackTrace()
        }
    }

    fun loadDataFromFileObjectOfObjects(fileName: String): Map<String, JsonObject> {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        return try {
            path.bufferedReader().use { reader ->
                val jsonContent = reader.readText()
                val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
                val jsonArrays = mutableMapOf<String, JsonObject>()
                for ((key, value) in jsonObject.entrySet()) {
                    if (value.isJsonObject) {
                        jsonArrays[key] = value.asJsonObject
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


    fun loadDataFromFileObject(fileName: String): Map<String, JsonArray> {
        val path = File(mc.mcDataDir, "config/noobroutes/$fileName.json")
        return try {
            path.bufferedReader().use { reader ->
                val jsonContent = reader.readText()
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

    private val customFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy_HH")
    private val customDate get(): String = LocalDateTime.now().format(customFormatter)

    fun backupFile(fileName: String) {
        try {
            File(mc.mcDataDir, "config/noobroutes/backups/$fileName").takeIf { !it.exists() }?.mkdirs()
            val source = Paths.get(mc.mcDataDir.toString(), "config/noobroutes/$fileName.json")
            val destination = Paths.get(mc.mcDataDir.toString(), "config/noobroutes/backups/$fileName/${customDate}.json")
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            logger.info("Error backing up ${fileName}, Object")
            e.printStackTrace()
        }
    }
}