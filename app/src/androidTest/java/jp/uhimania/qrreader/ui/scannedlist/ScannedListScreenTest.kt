package jp.uhimania.qrreader.ui.scannedlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import jp.uhimania.qrreader.R
import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import jp.uhimania.qrreader.domain.FormatDateUseCase
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test

class ScannedListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBulkRemove() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = FakeScannedResultRepository()
        val formatDateUseCase = FormatDateUseCase()
        val validateUrlUseCase = ValidateUrlUseCase()
        val viewModel = ScannedListViewModel(repository, formatDateUseCase, validateUrlUseCase)
        composeTestRule.setContent {
            ScannedListScreen(
                onStartScanning = {},
                onMoveToTrashBox = {},
                viewModel = viewModel
            )
        }

        // check initial value
        composeTestRule.onNodeWithText(context.getString(R.string.title_scanned_list)).assertExists()
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertExists()
        composeTestRule.onNodeWithText("ccc").assertExists()

        // switch to remove mode
        composeTestRule.onAllNodesWithContentDescription(Icons.Default.MoreVert.name)[3].performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.title_remove_mode)).performClick()

        // confirm remove mode
        composeTestRule.onNodeWithText(context.getString(R.string.title_remove_mode)).assertExists()

        // remove "aaa" and "ccc"
        composeTestRule.onAllNodes(isOff())[2].performClick()
        composeTestRule.onAllNodes(isOff())[0].performClick()
        composeTestRule.onNodeWithContentDescription(Icons.Default.Delete.name).performClick()

        // check results
        composeTestRule.onNodeWithText(context.getString(R.string.title_scanned_list)).assertExists()
        composeTestRule.onNodeWithText("aaa").assertDoesNotExist()
        composeTestRule.onNodeWithText("bbb").assertExists()
        composeTestRule.onNodeWithText("ccc").assertDoesNotExist()
    }

    @Test
    fun testClearSelection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = FakeScannedResultRepository()
        val formatDateUseCase = FormatDateUseCase()
        val validateUrlUseCase = ValidateUrlUseCase()
        val viewModel = ScannedListViewModel(repository, formatDateUseCase, validateUrlUseCase)
        composeTestRule.setContent {
            ScannedListScreen(
                onStartScanning = {},
                onMoveToTrashBox = {},
                viewModel = viewModel
            )
        }

        // switch to remove mode
        composeTestRule.onAllNodesWithContentDescription(Icons.Default.MoreVert.name)[3].performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.title_remove_mode)).performClick()

        // check not selected
        composeTestRule.onAllNodes(isOff()).assertCountEquals(3)

        // select "aaa" and "ccc"
        composeTestRule.onAllNodes(isOff())[2].performClick()
        composeTestRule.onAllNodes(isOff())[0].performClick()

        // check selected
        composeTestRule.onAllNodes(isOn()).assertCountEquals(2)
        composeTestRule.onAllNodes(isOff()).assertCountEquals(1)

        // end remove mode and reopen remove mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Check.name).performClick()
        composeTestRule.onAllNodesWithContentDescription(Icons.Default.MoreVert.name)[3].performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.title_remove_mode)).performClick()

        // check selection is cleared
        composeTestRule.onAllNodes(isOff()).assertCountEquals(3)
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        val flow = MutableStateFlow(listOf(
            ScannedResult(text = "aaa"),
            ScannedResult(text = "bbb"),
            ScannedResult(text = "ccc")
        ))
        override fun getResultsStream(): Flow<List<ScannedResult>> {
            return flow.asStateFlow()
        }

        override fun getDeletedResultsStream(): Flow<List<ScannedResult>> { return flowOf() }
        override suspend fun saveResult(result: ScannedResult) {}

        override suspend fun markAsDelete(id: String) {
            flow.update { flow.value.filterNot { it.id == id } }
        }

        override suspend fun unmarkAsDelete(id: String) {}
        override suspend fun forceDelete(id: String) {}
        override suspend fun purgeExpired() {}
        override suspend fun updateTitle(id: String, title: String) {}
    }
}