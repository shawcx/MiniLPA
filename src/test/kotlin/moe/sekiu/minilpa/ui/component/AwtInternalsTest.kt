package moe.sekiu.minilpa.ui.component

import java.awt.AWTEvent
import java.awt.event.FocusEvent
import java.lang.invoke.MethodHandles
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AwtInternalsTest
{
    private val consumed = MethodHandles.privateLookupIn(AWTEvent::class.java, MethodHandles.lookup())
        .findGetter(AWTEvent::class.java, "consumed", Boolean::class.javaPrimitiveType)

    @Test
    fun marksFocusEventsConsumed()
    {
        val event = FocusEvent(JPanel(), FocusEvent.FOCUS_GAINED)
        assertEquals(false, consumed.invoke(event) as Boolean)
        AwtInternals.consumed.invoke(event, true)
        assertEquals(true, consumed.invoke(event) as Boolean)
    }

    @Test
    fun resolvesFocusOwnerSetter()
    {
        assertNotNull(AwtInternals.focusOwner)
    }
}
