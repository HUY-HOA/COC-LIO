package com.huyhoa.coclio

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDisplayItemNameTest {
    @Test
    fun upgradeNotificationNamesDoNotExposeNumericDataIds() {
        assertEquals("Tháp cung", notificationDisplayItemName("Tháp cung"))
        assertEquals("Trang phục", notificationDisplayItemName("Trang phục #52000003"))
        assertEquals("Mục chưa được nhận diện", notificationDisplayItemName("Mục #52000003"))
    }
}
