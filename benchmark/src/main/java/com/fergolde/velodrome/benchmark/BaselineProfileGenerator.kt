package com.fergolde.velodrome.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(packageName = "com.fergolde.velodrome") {
            // Cold start
            pressHome()
            startActivityAndWait()

            // Wait for content to load
            device.wait(Until.hasObject(By.res("home")), 10_000)

            // Navigate through main screens
            device.findObject(By.text("Explore"))?.click()
            device.wait(Until.hasObject(By.res("explore")), 5_000)

            device.findObject(By.text("Settings"))?.click()
            device.wait(Until.hasObject(By.res("settings")), 5_000)

            device.findObject(By.text("Home"))?.click()
            device.wait(Until.hasObject(By.res("home")), 5_000)
        }
    }
}
