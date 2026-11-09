package com.mesut23.aistudio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test
    fun mimeTypeMatchesSavedImageExtension() {
        assertEquals("image/png", mimeOf(File("creation.png")))
        assertEquals("image/jpeg", mimeOf(File("creation.jpg")))
        assertEquals("image/jpeg", mimeOf(File("creation.jpeg")))
    }

    @Test
    fun aspectRatiosArePositiveAndNamed() {
        assertTrue(RATIOS.isNotEmpty())
        assertTrue(RATIOS.all { (label, ratio) -> label.isNotBlank() && ratio > 0f })
    }
}
