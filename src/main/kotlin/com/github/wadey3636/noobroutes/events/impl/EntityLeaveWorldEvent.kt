package com.github.wadey3636.noobroutes.events.impl

import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.Event

data class EntityLeaveWorldEvent(val entity: Entity) : Event()