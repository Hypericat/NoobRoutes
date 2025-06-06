package noobroutes.events.impl

import net.minecraftforge.fml.common.eventhandler.Event
import noobroutes.utils.skyblock.Island

class LocationChangeEvent(val location: Island) : Event() {

}