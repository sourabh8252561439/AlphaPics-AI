package com.example.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Immutable
data class EditorHistoryEntry(
    val id: Long,
    val label: String,
    val state: EditorState
)

/**
 * Manages an active non-destructive photo editing session.
 * Tracks current adjustments, undo/redo history, before/after comparison mode, and active tool.
 */
@Stable
class EditorSession(
    initialState: EditorState = EditorState()
) {
    var state by mutableStateOf(initialState)
        private set

    var workingState by mutableStateOf(initialState)

    private val undoStack = mutableStateListOf<EditorState>()
    private val redoStack = mutableStateListOf<EditorState>()
    private val operationHistory = mutableStateListOf(
        EditorHistoryEntry(id = 0L, label = "Original", state = initialState)
    )
    private var nextHistoryId = 1L

    var activeHistoryIndex by mutableIntStateOf(0)
        private set

    val historyEntries: List<EditorHistoryEntry>
        get() = operationHistory.toList()

    var isBeforeAfterActive by mutableStateOf(false)
    var isComparingSplit by mutableStateOf(false)
    var splitPosition by mutableStateOf(0.5f)

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Updates working state during slider adjustments without pushing to history immediately.
     */
    fun updateWorkingState(transform: (EditorState) -> EditorState) {
        workingState = transform(workingState)
    }

    /**
     * Commits the current working state to the official session state and saves an undo checkpoint.
     */
    fun commitWorkingState(label: String = "Edit") {
        if (workingState != state) {
            pushBounded(undoStack, state)
            redoStack.clear()
            state = workingState
            appendHistory(label, state)
        }
    }

    /**
     * Applies a direct state change, immediately pushing to undo history.
     */
    fun applyState(newState: EditorState, label: String = "Edit") {
        if (newState != state) {
            pushBounded(undoStack, state)
            redoStack.clear()
            state = newState
            workingState = newState
            appendHistory(label, state)
        }
    }

    /**
     * Reverts to previous state in history.
     */
    fun undo(): Boolean {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            pushBounded(redoStack, state)
            state = previous
            workingState = previous
            activeHistoryIndex = (activeHistoryIndex - 1).coerceAtLeast(0)
            return true
        }
        return false
    }

    /**
     * Re-applies next state from redo history.
     */
    fun redo(): Boolean {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            pushBounded(undoStack, state)
            state = next
            workingState = next
            activeHistoryIndex = (activeHistoryIndex + 1).coerceAtMost(operationHistory.lastIndex)
            return true
        }
        return false
    }

    /**
     * Resets working state back to committed state (e.g. user taps Cancel on a tool panel).
     */
    fun cancelWorkingState() {
        workingState = state
    }

    /**
     * Resets all adjustments back to default factory state.
     */
    fun resetAll() {
        if (!state.isDefault) {
            applyState(EditorState(), label = "Reset all")
        }
    }

    /** Restores any committed operation checkpoint and rebuilds linear undo/redo stacks. */
    fun jumpToHistory(entryId: Long): Boolean {
        val targetIndex = operationHistory.indexOfFirst { it.id == entryId }
        if (targetIndex < 0 || targetIndex == activeHistoryIndex) return false

        undoStack.clear()
        operationHistory.take(targetIndex).forEach { pushBounded(undoStack, it.state) }
        redoStack.clear()
        for (index in operationHistory.lastIndex downTo targetIndex + 1) {
            pushBounded(redoStack, operationHistory[index].state)
        }
        state = operationHistory[targetIndex].state
        workingState = state
        activeHistoryIndex = targetIndex
        return true
    }

    /**
     * Resets specific categories.
     */
    fun resetLight() {
        val newState = workingState.copy(light = LightAdjustments())
        updateWorkingState { newState }
    }

    fun resetColor() {
        val newState = workingState.copy(color = ColorAdjustments())
        updateWorkingState { newState }
    }

    fun resetColorMix() {
        val newState = workingState.copy(colorMix = ColorMixAdjustments())
        updateWorkingState { newState }
    }

    fun resetSplitTone() {
        val newState = workingState.copy(splitTone = SplitToneAdjustments())
        updateWorkingState { newState }
    }

    fun resetColorGrading() {
        val newState = workingState.copy(colorGrading = ColorGradingAdjustments())
        updateWorkingState { newState }
    }

    fun resetHsl() {
        val newState = workingState.copy(hsl = HslAdjustments())
        updateWorkingState { newState }
    }

    fun resetCurves() {
        val newState = workingState.copy(curves = CurvesAdjustments())
        updateWorkingState { newState }
    }

    fun resetDetail() {
        val newState = workingState.copy(detail = DetailAdjustments())
        updateWorkingState { newState }
    }

    fun resetEffects() {
        val newState = workingState.copy(effects = EffectAdjustments())
        updateWorkingState { newState }
    }

    fun resetTransform() {
        val newState = workingState.copy(transform = TransformAdjustments())
        updateWorkingState { newState }
    }

    fun resetRetouch() {
        val newState = workingState.copy(retouch = RetouchAdjustments())
        updateWorkingState { newState }
    }

    fun resetOverlays() {
        val newState = workingState.copy(overlays = OverlayAdjustments())
        updateWorkingState { newState }
    }

    fun resetFilter() {
        val newState = workingState.copy(filter = FilterAdjustment())
        updateWorkingState { newState }
    }

    private fun appendHistory(label: String, snapshot: EditorState) {
        while (operationHistory.lastIndex > activeHistoryIndex) {
            operationHistory.removeAt(operationHistory.lastIndex)
        }
        operationHistory.add(
            EditorHistoryEntry(
                id = nextHistoryId++,
                label = label.trim().take(48).ifBlank { "Edit" },
                state = snapshot
            )
        )
        while (operationHistory.size > MAX_HISTORY_ENTRIES) {
            operationHistory.removeAt(1)
        }
        activeHistoryIndex = operationHistory.lastIndex
    }

    private fun pushBounded(stack: MutableList<EditorState>, snapshot: EditorState) {
        if (stack.size == MAX_HISTORY_ENTRIES) stack.removeAt(0)
        stack.add(snapshot)
    }

    companion object {
        const val MAX_HISTORY_ENTRIES = 64
    }
}

@Composable
fun rememberEditorSession(
    initialState: EditorState = EditorState()
): EditorSession {
    return remember { EditorSession(initialState) }
}
