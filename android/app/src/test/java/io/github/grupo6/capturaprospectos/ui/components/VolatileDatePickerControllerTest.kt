package io.github.grupo6.capturaprospectos.ui.components

import org.junit.Test
import org.junit.Assert.*

class VolatileDatePickerControllerTest {
    @Test fun closeAndFreshOwnerForgetDraftAndItsInputMode() {
        val controller = VolatileDatePickerController()
        controller.open(DatePickerTarget.BIRTH, "2000-01-01")
        val first = controller.draft.value!!
        controller.update(first.id, first.selectedMillis, 946684800000L, true)
        assertTrue(controller.draft.value!!.inputMode)
        assertEquals("DatePickerDraft(redacted)", controller.draft.value.toString())
        controller.close()
        assertNull(controller.draft.value)
        assertNull(VolatileDatePickerController().draft.value)
        controller.open(DatePickerTarget.BIRTH, "")
        assertNull(controller.draft.value!!.selectedMillis)
        assertFalse(controller.draft.value!!.inputMode)
    }
    @Test fun latePickerUpdateCannotOverwriteNewDialogOrReopenCancelledOne() {
        val controller = VolatileDatePickerController()
        controller.open(DatePickerTarget.FROM, "2000-01-01")
        val first = controller.draft.value!!
        controller.open(DatePickerTarget.UNTIL, "2000-01-03")
        val second = controller.draft.value
        controller.update(first.id, null, 0, true)
        assertEquals(second, controller.draft.value)
        controller.close()
        controller.update(second!!.id, null, 0, true)
        assertNull(controller.draft.value)
    }
}
