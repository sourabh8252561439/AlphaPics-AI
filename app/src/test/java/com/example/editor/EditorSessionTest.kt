package com.example.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSessionTest {

    @Test
    fun `initial session starts in default state with empty undo and redo`() {
        val session = EditorSession()
        assertTrue(session.state.isDefault)
        assertFalse(session.canUndo)
        assertFalse(session.canRedo)
    }

    @Test
    fun `commitWorkingState pushes to undo stack and updates committed state`() {
        val session = EditorSession()
        session.updateWorkingState { it.copy(light = it.light.copy(exposure = 25f)) }

        assertEquals(0f, session.state.light.exposure, 0.01f)
        assertEquals(25f, session.workingState.light.exposure, 0.01f)

        session.commitWorkingState()

        assertEquals(25f, session.state.light.exposure, 0.01f)
        assertTrue(session.canUndo)
        assertFalse(session.canRedo)
    }

    @Test
    fun `undo and redo traverse history correctly`() {
        val session = EditorSession()
        session.applyState(session.state.copy(light = LightAdjustments(contrast = 15f)))
        session.applyState(session.state.copy(color = ColorAdjustments(warmth = 30f)))

        assertEquals(15f, session.state.light.contrast, 0.01f)
        assertEquals(30f, session.state.color.warmth, 0.01f)
        assertTrue(session.canUndo)

        // Undo to step 1
        assertTrue(session.undo())
        assertEquals(15f, session.state.light.contrast, 0.01f)
        assertEquals(0f, session.state.color.warmth, 0.01f)
        assertTrue(session.canRedo)

        // Undo to initial
        assertTrue(session.undo())
        assertTrue(session.state.isDefault)
        assertFalse(session.canUndo)
        assertTrue(session.canRedo)

        // Redo back to step 1
        assertTrue(session.redo())
        assertEquals(15f, session.state.light.contrast, 0.01f)

        // Redo back to step 2
        assertTrue(session.redo())
        assertEquals(30f, session.state.color.warmth, 0.01f)
        assertFalse(session.canRedo)
    }

    @Test
    fun `cancelWorkingState restores committed state`() {
        val session = EditorSession()
        session.applyState(session.state.copy(light = LightAdjustments(exposure = 10f)))

        session.updateWorkingState { it.copy(light = it.light.copy(exposure = 50f)) }
        assertEquals(50f, session.workingState.light.exposure, 0.01f)

        session.cancelWorkingState()
        assertEquals(10f, session.workingState.light.exposure, 0.01f)
    }

    @Test
    fun `resetAll resets all modifications and pushes previous state to undo`() {
        val session = EditorSession()
        session.applyState(
            session.state.copy(
                light = LightAdjustments(exposure = 20f),
                color = ColorAdjustments(saturation = 40f)
            )
        )

        session.resetAll()
        assertTrue(session.state.isDefault)
        assertTrue(session.canUndo)

        session.undo()
        assertEquals(20f, session.state.light.exposure, 0.01f)
        assertEquals(40f, session.state.color.saturation, 0.01f)
    }

    @Test
    fun `preset catalog returns known filters and falls back safely`() {
        val clean = FilterPresetCatalog.find("clean")
        assertEquals("Clean", clean.label)
        assertEquals(8f, clean.contrast, 0.01f)

        val unknown = FilterPresetCatalog.find("non-existent-filter")
        assertEquals("Original", unknown.label)
    }

    @Test
    fun `HSL and curves reset independently in working state`() {
        val session = EditorSession(
            EditorState(
                hsl = HslAdjustments(red = HslChannelAdjustment(hue = 25f)),
                curves = CurvesAdjustments(
                    master = ToneCurve(
                        listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.7f), CurvePoint(1f, 1f))
                    )
                )
            )
        )

        session.resetHsl()
        assertTrue(session.workingState.hsl.isNeutral)
        assertFalse(session.workingState.curves.isNeutral)

        session.resetCurves()
        assertTrue(session.workingState.curves.isNeutral)
    }

    @Test
    fun `color mix split tone and grading reset independently`() {
        val session = EditorSession(
            EditorState(
                colorMix = ColorMixAdjustments(red = 20f),
                splitTone = SplitToneAdjustments(shadowSaturation = 30f),
                colorGrading = ColorGradingAdjustments(
                    highlights = ColorGradeRange(hue = 45f, luminance = 20f)
                )
            )
        )

        session.resetColorMix()
        assertTrue(session.workingState.colorMix.isNeutral)
        assertFalse(session.workingState.splitTone.isNeutral)

        session.resetSplitTone()
        assertTrue(session.workingState.splitTone.isNeutral)
        assertFalse(session.workingState.colorGrading.isNeutral)

        session.resetColorGrading()
        assertTrue(session.workingState.colorGrading.isNeutral)
    }

    @Test
    fun `retouch reset clears strokes without changing other categories`() {
        val session = EditorSession(
            EditorState(
                retouch = RetouchAdjustments(
                    strokes = listOf(
                        RetouchStroke(
                            mode = LocalRetouchMode.HEAL,
                            points = listOf(RetouchPoint(0.5f, 0.5f)),
                            size = 8f,
                            feather = 50f,
                            strength = 60f
                        )
                    )
                ),
                detail = DetailAdjustments(clarity = 20f)
            )
        )

        session.resetRetouch()

        assertTrue(session.workingState.retouch.isNeutral)
        assertEquals(20f, session.workingState.detail.clarity, 0.01f)
    }

    @Test
    fun `named history supports jump redo and branch replacement`() {
        val session = EditorSession()
        session.updateWorkingState { it.copy(light = LightAdjustments(exposure = 12f)) }
        session.commitWorkingState("Light")
        session.updateWorkingState { it.copy(color = ColorAdjustments(warmth = 18f)) }
        session.commitWorkingState("Color")

        assertEquals(listOf("Original", "Light", "Color"), session.historyEntries.map { it.label })
        val lightEntry = session.historyEntries[1]
        assertTrue(session.jumpToHistory(lightEntry.id))
        assertEquals(12f, session.state.light.exposure, 0.01f)
        assertEquals(0f, session.state.color.warmth, 0.01f)
        assertTrue(session.canRedo)

        assertTrue(session.redo())
        assertEquals(18f, session.state.color.warmth, 0.01f)
        session.jumpToHistory(lightEntry.id)
        session.updateWorkingState { it.copy(detail = DetailAdjustments(clarity = 24f)) }
        session.commitWorkingState("Detail")

        assertEquals(listOf("Original", "Light", "Detail"), session.historyEntries.map { it.label })
        assertFalse(session.canRedo)
    }

    @Test
    fun `named history remains bounded while preserving original entry`() {
        val session = EditorSession()
        repeat(EditorSession.MAX_HISTORY_ENTRIES + 10) { index ->
            session.applyState(
                session.state.copy(light = session.state.light.copy(brightness = index + 1f)),
                label = "Step $index"
            )
        }

        assertEquals(EditorSession.MAX_HISTORY_ENTRIES, session.historyEntries.size)
        assertEquals("Original", session.historyEntries.first().label)
        assertEquals(session.historyEntries.lastIndex, session.activeHistoryIndex)
    }
}
