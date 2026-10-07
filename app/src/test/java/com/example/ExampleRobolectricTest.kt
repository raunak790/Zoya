package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.tools.ToolExecutionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zoya", appName)
    }

    @Test
    fun `verify tool execution engine declares required tools`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = ToolExecutionEngine(context)
        val toolsJson = engine.getToolsDeclarationJson()

        assertNotNull(toolsJson)
        assertTrue(toolsJson.length() > 0)

        val firstToolObj = toolsJson.getJSONObject(0)
        val funcDecls = firstToolObj.getJSONArray("functionDeclarations")
        val declaredNames = mutableListOf<String>()
        for (i in 0 until funcDecls.length()) {
            val decl = funcDecls.getJSONObject(i)
            declaredNames.add(decl.getString("name"))
        }

        assertTrue(declaredNames.contains("openApp"))
        assertTrue(declaredNames.contains("searchAndCallContact"))
        assertTrue(declaredNames.contains("sendWhatsAppMessage"))
        assertTrue(declaredNames.contains("sendGmail"))
    }
}
