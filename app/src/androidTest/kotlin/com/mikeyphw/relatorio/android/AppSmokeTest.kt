package com.mikeyphw.relatorio.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    private val dataResetRule = TestAppDataResetRule()
    private val activityRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(dataResetRule).around(activityRule)

    private val composeRule get() = activityRule

    @Test
    fun opensHomeAndStartsTheDefaultReportFlow() {
        composeRule.onNodeWithText("Preencher novo relatório").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Começar em branco").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Relatório AD Timóteo").assertIsDisplayed()
        composeRule.onNodeWithText("Padrão").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Dados do relatório").assertIsDisplayed()
    }

    @Test
    fun completeTemplateRemainsSelectable() {
        composeRule.onNodeWithText("Preencher novo relatório").performClick()
        composeRule.onNodeWithText("Começar em branco").performClick()
        composeRule.onNodeWithText("Relatório Completo").performClick()
        composeRule.onNodeWithText("74 campos numéricos").assertIsDisplayed()
    }
}
