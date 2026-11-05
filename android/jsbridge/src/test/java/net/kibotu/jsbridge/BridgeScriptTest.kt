package net.kibotu.jsbridge

import net.kibotu.jsbridge.commands.BridgeCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BridgeScriptTest {

    private class Fake(override val action: String, override val features: Set<String> = setOf(action)) : BridgeCommand {
        override suspend fun handle(content: Any?): Any? = null
    }

    @Test
    fun `a command contributes its action by default`() {
        assertEquals(listOf("haptic"), featureNames(listOf(Fake("haptic"))))
    }

    @Test
    fun `declared sub-features are added and the result is sorted and unique`() {
        val commands = listOf(
            Fake("haptic", setOf("haptic", "haptic.pattern")),
            Fake("deviceInfo"),
            Fake("haptic"),
        )

        assertEquals(listOf("deviceInfo", "haptic", "haptic.pattern"), featureNames(commands))
    }

    @Test
    fun `no commands means no features`() {
        assertEquals(emptyList<String>(), featureNames(emptyList()))
    }

    @Test
    fun `template placeholders are replaced`() {
        val script = renderScript("name=__BRIDGE_NAME__ f=__FEATURES__", "bridge", listOf(Fake("toast")))

        assertEquals("name=bridge f=[\"toast\"]", script)
        assertFalse(script.contains("__"))
    }
}
