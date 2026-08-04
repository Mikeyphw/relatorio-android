package com.mikeyphw.relatorio.android

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Starts every UI scenario with an empty Room database and default preferences. */
class TestAppDataResetRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val application = InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext as RelatorioApplication
            runBlocking {
                application.container.repository.clearAllForTesting()
                application.container.preferences.clearAllForTesting()
            }
            base.evaluate()
        }
    }
}
