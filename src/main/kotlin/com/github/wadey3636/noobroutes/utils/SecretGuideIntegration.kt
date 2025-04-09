package com.github.wadey3636.noobroutes.utils

import me.defnotstolen.Core.logger
import java.lang.reflect.Field

object SecretGuideIntegration {
    fun setSecretGuideAura(value: Boolean) {
        try {
            val field: Field =
                Class.forName("com.github.soshimee.secretguide.config.SecretGuideConfig")
                    .getDeclaredField("secretAuraEnabled")

            field.isAccessible = true
            field.set(null, value)
        } catch (e: ClassNotFoundException) {
            logger.info("Set:Secret Guide Not Found")
        } catch (e: NoSuchFieldException) {
            logger.info("Set:Secret Guide Aura Variable Not Found")
        } catch (e: IllegalAccessException) {
            logger.info("Set:Couldn't Access Variable")
        }
    }
    fun getSecretGuideAura(): Boolean? {
        try {
            val field: Field =
                Class.forName("com.github.soshimee.secretguide.config.SecretGuideConfig")
                    .getDeclaredField("secretAuraEnabled")

            field.isAccessible = true
            val aura = field.get(null)
            if (aura is Boolean) {
                return aura
            }


        } catch (e: ClassNotFoundException) {
            logger.info("Get:Secret Guide Not Found")
        } catch (e: NoSuchFieldException) {
            logger.info("Get:Secret Guide Aura Variable Not Found")
        } catch (e: IllegalAccessException) {
            logger.info("Get:Couldn't Access Variable")
        }
        return null
    }

}