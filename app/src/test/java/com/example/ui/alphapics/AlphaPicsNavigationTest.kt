package com.example.ui.alphapics

import com.example.ui.alphapics.navigation.AlphaPicsDestination
import com.example.ui.alphapics.navigation.AlphaPicsFeatureCatalog
import com.example.ui.alphapics.navigation.AlphaPicsNavState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphaPicsNavigationTest {

    @Test
    fun `initial state starts at Home and cannot pop`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Home)
        assertEquals(AlphaPicsDestination.Home, navState.currentDestination)
        assertFalse(navState.canGoBack)
        assertFalse(navState.pop())
    }

    @Test
    fun `navigateTo pushes new destination and enables pop`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Home)
        navState.navigateTo(AlphaPicsDestination.Editor("adjust"))

        assertEquals(AlphaPicsDestination.Editor("adjust"), navState.currentDestination)
        assertTrue(navState.canGoBack)

        assertTrue(navState.pop())
        assertEquals(AlphaPicsDestination.Home, navState.currentDestination)
        assertFalse(navState.canGoBack)
    }

    @Test
    fun `duplicate navigateTo call does not push extra backstack entry`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Home)
        navState.navigateTo(AlphaPicsDestination.Compressor)
        navState.navigateTo(AlphaPicsDestination.Compressor)

        assertEquals(AlphaPicsDestination.Compressor, navState.currentDestination)
        assertTrue(navState.pop())
        assertEquals(AlphaPicsDestination.Home, navState.currentDestination)
        assertFalse(navState.canGoBack)
    }

    @Test
    fun `collage is a typed destination that returns to the editor`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Editor())
        navState.navigateTo(AlphaPicsDestination.Collage)

        assertEquals(AlphaPicsDestination.Collage, navState.currentDestination)
        assertTrue(navState.pop())
        assertEquals(AlphaPicsDestination.Editor(), navState.currentDestination)
    }

    @Test
    fun `photo utilities preserve their initial tab through typed navigation`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Home)
        navState.navigateTo(AlphaPicsDestination.PhotoUtilities("convert"))

        assertEquals(
            AlphaPicsDestination.PhotoUtilities("convert"),
            navState.currentDestination
        )
        assertTrue(navState.pop())
        assertEquals(AlphaPicsDestination.Home, navState.currentDestination)
    }

    @Test
    fun `batch studio is a separate typed destination behind protected batch compress`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Home)
        navState.navigateTo(AlphaPicsDestination.Batch)
        navState.navigateTo(AlphaPicsDestination.BatchStudio)

        assertEquals(AlphaPicsDestination.BatchStudio, navState.currentDestination)
        assertTrue(navState.pop())
        assertEquals(AlphaPicsDestination.Batch, navState.currentDestination)
    }

    @Test
    fun `popToRoot returns directly to root destination`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Home)
        navState.navigateTo(AlphaPicsDestination.Enhance("ai-enhance"))
        navState.navigateTo(AlphaPicsDestination.Editor("crop"))
        navState.navigateTo(AlphaPicsDestination.Settings)

        assertEquals(AlphaPicsDestination.Settings, navState.currentDestination)
        navState.popToRoot()

        assertEquals(AlphaPicsDestination.Home, navState.currentDestination)
        assertFalse(navState.canGoBack)
    }

    @Test
    fun `replace replaces current top destination`() {
        val navState = AlphaPicsNavState(AlphaPicsDestination.Home)
        navState.navigateTo(AlphaPicsDestination.Placeholder(AlphaPicsFeatureCatalog.Resize))
        navState.replace(AlphaPicsDestination.Placeholder(AlphaPicsFeatureCatalog.Convert))

        assertEquals(
            AlphaPicsDestination.Placeholder(AlphaPicsFeatureCatalog.Convert),
            navState.currentDestination
        )
        assertTrue(navState.pop())
        assertEquals(AlphaPicsDestination.Home, navState.currentDestination)
    }
}
