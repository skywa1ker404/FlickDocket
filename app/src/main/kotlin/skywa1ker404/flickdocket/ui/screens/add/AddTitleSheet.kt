@file:OptIn(ExperimentalMaterial3Api::class)

package skywa1ker404.flickdocket.ui.screens.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.*
import skywa1ker404.flickdocket.ui.components.*
import java.util.Calendar

/** Manual, local-only entry. Works with no account and no provider connected. */
@Composable
fun AddTitleSheet(onDismiss: () -> Unit, onSave: (NewTitleDraft) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(MediaType.MOVIE) }
    var year by rememberSaveable { mutableStateOf("") }
    var runtime by rememberSaveable { mutableStateOf("") }
    var episodes by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(WatchStatus.PLANNING) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    val draft = NewTitleDraft(name, type, year.toIntOrNull(), runtime.toIntOrNull(), if (type == MediaType.TV) episodes.toIntOrNull() else null, status)
    val numbersParse = listOf(year, runtime, episodes).all { it.isEmpty() || it.toIntOrNull() != null }
    val issues = DraftValidation.validate(draft, currentYear)
    val canSave = numbersParse && issues.isEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.add_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.field_title)) })
            PillRow(MediaType.entries, { it == type }, { mediaTypeLabel(it) }, { type = it }, Modifier.padding(horizontal = 0.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(year, { year = it }, R.string.field_year, Modifier.weight(1f), isError = DraftIssue.YearOutOfRange in issues)
                NumberField(runtime, { runtime = it }, R.string.field_runtime, Modifier.weight(1f))
                if (type == MediaType.TV) NumberField(episodes, { episodes = it }, R.string.field_episodes, Modifier.weight(1f))
            }
            PillRow(WatchStatus.entries, { it == status }, { statusLabel(it) }, { status = it })
            Button({ onSave(draft.copy(name = name.trim())) }, Modifier.fillMaxWidth(), enabled = canSave) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: Int, modifier: Modifier, isError: Boolean = false) {
    OutlinedTextField(
        value, { onChange(it.filter(Char::isDigit).take(5)) }, modifier, singleLine = true, isError = isError,
        label = { Text(stringResource(label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
