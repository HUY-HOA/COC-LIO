package com.huyhoa.coclio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainActivityNavigationTest {
    @Test
    fun backFromSettingsRestoresThePreviouslySelectedJsonSection() {
        assertEquals(2 to 2, previousCocLioNavigationState(selectedTab = 4, jsonSection = 2))
    }

    @Test
    fun backFromJsonDetailsReturnsToOverview() {
        assertEquals(2 to 0, previousCocLioNavigationState(selectedTab = 2, jsonSection = 1))
        assertEquals(2 to 0, previousCocLioNavigationState(selectedTab = 2, jsonSection = 2))
    }

    @Test
    fun backAtRootRequiresExplicitExitConfirmation() {
        assertNull(previousCocLioNavigationState(selectedTab = 2, jsonSection = 0))
        assertEquals(true, shouldConfirmCocLioExit(selectedTab = 2, jsonSection = 0))
    }

    @Test
    fun backFromNestedScreenNavigatesInsteadOfRequestingExitConfirmation() {
        assertEquals(false, shouldConfirmCocLioExit(selectedTab = 4, jsonSection = 2))
        assertEquals(false, shouldConfirmCocLioExit(selectedTab = 2, jsonSection = 1))
    }
}
