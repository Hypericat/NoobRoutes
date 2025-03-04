package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.adapters.RingsMapTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import me.odinmain.OdinMain.logger
import me.odinmain.OdinMain.mc
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import me.odinmain.config.DataManager
import me.odinmain.features.settings.impl.StringSetting
import me.odinmain.utils.LookVec

enum class RingTypes {
    WALK,
    MOTION,
    HCLIP
}



data class Ring (
    val type: RingTypes,
    val coords: Vec3 = Vec3(mc.thePlayer?.posX ?: 0.0, mc.thePlayer?.posY ?: 0.0, mc.thePlayer?.posZ ?: 0.0),
    val direction: LookVec = LookVec(mc.thePlayer?.rotationYaw ?: 0f, mc.thePlayer?.rotationPitch ?: 0f),
    val walk: Boolean = false,
    val look: Boolean = false,
    val center: Boolean = false
)

object AutoP3: Module (
    name = "AutoP3",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "AutoP3"
) {
    val gson = GsonBuilder().registerTypeAdapter(
        object : TypeToken<MutableMap<String, MutableList<Ring>>>() {}.type,
        RingsMapTypeAdapter()
    ).setPrettyPrinting().create()

    private val route by StringSetting("Route", "", description = "Route to use")
    var rings = mutableMapOf<String, MutableList<Ring>>()




    fun addRing(args: Array<out String>?) {
        if (args.isNullOrEmpty()) return modMessage("need args stoopid")
        when(args[0]) {
            "add" -> addNormalRing(args)
            "blink" -> modMessage("coming soon")
        }
    }

    private fun addNormalRing(args: Array<out String>?) {
        modMessage("added")
        logger.info(route)
        if (route.isEmpty()) return
        rings[route]?.add(Ring(RingTypes.WALK)) ?: run { rings[route] = mutableListOf(Ring(RingTypes.WALK)) }
        saveRings()
    }

    private fun saveRings(){
        try {
            val jsonArray = JsonArray().apply {
                rings.forEach { (routeName, ringList) ->
                    add(JsonObject().apply {
                        addProperty("route", routeName)
                        add("rings", gson.toJsonTree(ringList))
                    })
                }
            }
            DataManager.saveDataToFile("rings", jsonArray)
        } catch (e: Exception) {
            modMessage("error saving")
            logger.error("error saving rings", e)
        }

    }

    fun loadRings(){
        rings.clear()
        val jsonObjects = DataManager.loadDataFromFile("rings")
        jsonObjects.forEach { jsonObject ->
            val routeName = jsonObject.get("route").asString
            val ringList = gson.fromJson<MutableList<Ring>>(jsonObject.get("rings"), object : TypeToken<MutableList<Ring>>() {}.type)
            rings[routeName] = ringList
        }
    }


}