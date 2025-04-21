package com.github.wadey3636.noobroutes.utils

import me.noobmodcore.Core.logger
import java.lang.reflect.Field

object SecretGuideIntegration {
    /**
     * Updates the state of the secret guide aura by modifying the relevant configuration field.
     *
     * @param value Boolean value representing whether the secret guide aura should be enabled (true) or disabled (false).
     */
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

    /**
     * Retrieves the value of the `secretAuraEnabled` configuration field from the `SecretGuideConfig` class
     * using reflection.
     *
     * @return The value of the `secretAuraEnabled` field as a Boolean if it exists and is accessible,
     *         or null if the field cannot be found, accessed, or if an error occurs during the process.
     */
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