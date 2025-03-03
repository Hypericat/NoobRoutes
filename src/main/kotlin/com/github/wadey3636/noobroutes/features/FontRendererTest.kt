package com.github.wadey3636.noobroutes.features

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.render.ClickGUIModule
import me.odinmain.utils.render.text
import me.odinmain.utils.skyblock.modMessage
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FontRendererTest : Module(
    name = "Font Render",
    description = "Test",
    category = Category.RENDER
) {

    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent.Post) {
        text("Font Test", 300, 300, ClickGUIModule.color, 10)
    }

    override fun onEnable() {
        super.onEnable()
        modMessage("Enabled Font Tester")
    }

}