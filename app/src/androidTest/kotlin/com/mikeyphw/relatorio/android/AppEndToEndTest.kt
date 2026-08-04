package com.mikeyphw.relatorio.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppEndToEndTest {
    private val dataResetRule = TestAppDataResetRule()
    private val activityRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(dataResetRule).around(activityRule)

    private val composeRule get() = activityRule

    @Test
    fun completesMonthlyReportAndReachesPdfHistoryAndBackup() {
        composeRule.onNodeWithText("Ajuda").performClick()
        composeRule.onNodeWithText("Como começar").assertIsDisplayed()
        composeRule.onNodeWithText("Entendi").performClick()

        composeRule.onNodeWithText("Preencher novo relatório").performClick()
        composeRule.onNodeWithText("Começar em branco").performClick()
        composeRule.onNodeWithText("Relatório AD Timóteo").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()

        composeRule.onNodeWithTag("identity_congregation")
            .performTextReplacement("Central de Teste")
        composeRule.onNodeWithTag("identity_save")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Relatório em andamento").assertIsDisplayed()

        composeRule.onNodeWithText("Começar preenchimento")
            .performScrollTo()
            .performClick()
        repeat(5) {
            composeRule.onNodeWithTag("numeric_next")
                .performScrollTo()
                .performClick()
        }
        composeRule.onNodeWithText("Números preenchidos").assertIsDisplayed()

        composeRule.onNodeWithText("Movimentação de membros")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Adicionar pessoa")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("movement_name")
            .performTextReplacement("Pessoa de Teste")
        composeRule.onNodeWithTag("movement_save")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Pessoa de Teste").assertIsDisplayed()
        composeRule.onNodeWithText("Voltar").performClick()

        composeRule.onNodeWithText("Anotações e sugestões")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("notes_input")
            .performTextReplacement("Anotação de regressão ponta a ponta.")
        composeRule.onNodeWithText("Voltar").performClick()

        composeRule.onNodeWithText("Revisar e concluir")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("review_complete")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Sim, concluir").performClick()

        composeRule.onNodeWithText("PDF do relatório").assertIsDisplayed()
        composeRule.onNodeWithText("Visualizar PDF").assertIsDisplayed()
        composeRule.onNodeWithText("Imprimir").assertIsDisplayed()

        composeRule.onNodeWithText("Voltar").performClick()
        composeRule.onNodeWithText("Central de Teste").assertIsDisplayed()
        composeRule.onNodeWithText("Voltar ao início")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Mais opções").performClick()
        composeRule.onNodeWithText("Backup e restauração").performClick()
        composeRule.onNodeWithText("Salvar backup dos dados").assertIsDisplayed()
        composeRule.onNodeWithText("Escolher arquivo de backup").assertIsDisplayed()
    }
}
