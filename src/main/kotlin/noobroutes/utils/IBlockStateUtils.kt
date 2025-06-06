package noobroutes.utils

import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import noobroutes.utils.skyblock.devMessage


object IBlockStateUtils {

    val airIBlockState = Block.getStateById(0)

    fun getBlockStateFromName(name: String): BlockState? {
        return Block.getBlockFromName(name)?.blockState
    }

    @Suppress("UNCHECKED_CAST")
    fun IBlockState.setProperty(propertyName: String, value: Any): IBlockState? {
        val property = this.properties.keys.firstOrNull {it.name == propertyName}
        if (property == null) {
            devMessage("Invalid Property")
            return null
        }

        val typedProperty = property as IProperty<Comparable<Any>>
        if (!typedProperty.allowedValues.contains(value)) {
            devMessage("Invalid Value: $value, allowed values: ${property.allowedValues}")
            return null
        }

        return this.withProperty(typedProperty, value as Comparable<Any>)
    }

}