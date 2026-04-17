package jp.uhimania.qrreader.ui.scannedlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
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

    @Test
    fun testSearch() {
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

        // initial value
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertExists()
        composeTestRule.onNodeWithText("ccc").assertExists()

        // switch to search mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // do search
        val query1 = "aa"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query1)
        composeTestRule.onNodeWithText(query1).performImeAction()

        // check search result
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertDoesNotExist()
        composeTestRule.onNodeWithText("ccc").assertDoesNotExist()

        // do search
        val query2 = "tit"
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query2)
        composeTestRule.onNodeWithText(query2).performImeAction()

        // check search result
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertExists()
        composeTestRule.onNodeWithText("ccc").assertDoesNotExist()

        // end search mode
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()

        // check not filtered
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertExists()
        composeTestRule.onNodeWithText("ccc").assertExists()
    }

    @Test
    fun testSearchHistory() {
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

        // switch to search mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // do search
        val query1 = "query1"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query1)
        composeTestRule.onNodeWithText(query1).performImeAction()

        // check history added
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performClick()
        composeTestRule.onNodeWithText(query1).assertExists()

        // do search
        val query2 = "query2"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query2)
        composeTestRule.onNodeWithText(query2).performImeAction()

        // check history added
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performClick()
        composeTestRule.onNodeWithText(query1).assertExists()
        composeTestRule.onNodeWithText(query2).assertExists()

        // input and back
        val query3 = "query3"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query3)
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()

        // check history not added
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performClick()
        composeTestRule.onNodeWithText(query1).assertExists()
        composeTestRule.onNodeWithText(query2).assertExists()
        composeTestRule.onNodeWithText(query3).assertDoesNotExist()

        // input and back key
        val query4 = "query4"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query4)
        Espresso.pressBack()

        // check history not added
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performClick()
        composeTestRule.onNodeWithText(query1).assertExists()
        composeTestRule.onNodeWithText(query2).assertExists()
        composeTestRule.onNodeWithText(query3).assertDoesNotExist()
        composeTestRule.onNodeWithText(query4).assertDoesNotExist()

        // end search mode and reopen search mode
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // check empty history not added
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query4)
        composeTestRule.onNodeWithText("").assertDoesNotExist()
    }

    @Test
    fun testSearchByHistory() {
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

        // switch to search mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // do search
        val query = "aa"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query)
        composeTestRule.onNodeWithText(query).performImeAction()

        // check search result
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertDoesNotExist()
        composeTestRule.onNodeWithText("ccc").assertDoesNotExist()

        // clear search query
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).performClick()

        // check search result
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertExists()
        composeTestRule.onNodeWithText("ccc").assertExists()

        // search by history
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performClick()
        composeTestRule.onNodeWithText(query).performClick()

        // check search result
        composeTestRule.onNodeWithText("aaa").assertExists()
        composeTestRule.onNodeWithText("bbb").assertDoesNotExist()
        composeTestRule.onNodeWithText("ccc").assertDoesNotExist()
    }

    @Test
    fun testSearchBackButton() {
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

        // switch to search mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // end input
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()

        // confirm placeholder
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).assertExists()

        // end search mode
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()

        // confirm search mode is closed
        composeTestRule.onNodeWithText(context.getString(R.string.title_scanned_list)).assertExists()
    }

    @Test
    fun testSearchClearButton() {
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

        // switch to search mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // confirm clear button is not displayed
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).assertDoesNotExist()

        // input query and clear button appeared
        val query = "query"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query)
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).assertExists()

        // clear input and clear button go away
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).performClick()
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).assertDoesNotExist()
        composeTestRule.onNodeWithText(query).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).assertExists()

        // fix input and clear button is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query)
        composeTestRule.onNodeWithText(query).performImeAction()
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).assertExists()

        // clear input and clear button go away
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).performClick()
        composeTestRule.onNodeWithContentDescription(Icons.Default.Clear.name).assertDoesNotExist()
        composeTestRule.onNodeWithText(query).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).assertExists()
    }

    @Test
    fun testCancelSearch() {
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

        // switch to search mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // input query
        val query = "query"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query)

        // back button
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()

        // check input is cleared
        composeTestRule.onNodeWithText(query).assertDoesNotExist()

        // input query
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query)

        // back key
        Espresso.pressBack()

        // check input is cleared
        composeTestRule.onNodeWithText(query).assertDoesNotExist()
    }

    @Test
    fun testResetQuery() {
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

        // switch to search mode
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // do search
        val query = "query"
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).performTextInput(query)
        composeTestRule.onNodeWithText(query).performImeAction()

        // end search mode and reopen search mode
        composeTestRule.onNodeWithContentDescription(Icons.AutoMirrored.Filled.ArrowBack.name).performClick()
        composeTestRule.onNodeWithContentDescription(Icons.Default.Search.name).performClick()

        // check query is reset
        composeTestRule.onNodeWithText(context.getString(R.string.text_search_results)).assertExists()
        composeTestRule.onAllNodesWithText(query).assertCountEquals(1)
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        val flow = MutableStateFlow(listOf(
            ScannedResult(text = "aaa", title = "title1"),
            ScannedResult(text = "bbb", title = "title2"),
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