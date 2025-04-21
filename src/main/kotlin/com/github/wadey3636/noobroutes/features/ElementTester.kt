package com.github.wadey3636.noobroutes.features

import me.modcore.features.Category
import me.modcore.features.Module
import me.modcore.features.settings.impl.*
import me.modcore.utils.render.Color

object ElementTester : Module(
    name = "Element Tester",
    category = Category.FLOOR7,
    description = "Tests elements."
){
    private val testColor by ColorSetting("Test Color", description = "gyatt", default = Color.WHITE)
    private val testSlider by NumberSetting(name = "Test Numba", description = "rah", min = 0f, max = 100f, default = 50f)
    private val testDropDown by DropdownSetting(name = "Test Dropdown")
    private val testBoolean by BooleanSetting("Test Boolean", default = true, description = "yarr")
    private val testDual by DualSetting(name = "Test Dual", description = "gay", default = true, left = "Left", right = "Right")


}