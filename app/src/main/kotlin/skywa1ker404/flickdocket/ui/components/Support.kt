package skywa1ker404.flickdocket.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.MetadataError
import skywa1ker404.flickdocket.domain.RefSource

@Composable
fun metadataErrorText(e: MetadataError): String = stringResource(
    when (e) {
        MetadataError.MissingCredentials -> R.string.err_no_key
        MetadataError.Unauthorized -> R.string.err_unauthorized
        MetadataError.NotFound -> R.string.err_not_found
        is MetadataError.RateLimited -> R.string.err_rate_limited
        MetadataError.Offline -> R.string.err_offline
        is MetadataError.Server -> R.string.err_server
        MetadataError.Malformed -> R.string.err_malformed
        MetadataError.Unsupported -> R.string.err_unsupported
    }
)

@Composable
fun SearchField(text: String, onText: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = text, onValueChange = onText, modifier = modifier, singleLine = true, shape = RoundedCornerShape(50),
        placeholder = { Text(placeholder) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = if (text.isNotEmpty()) { { IconButton({ onText("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.clear)) } } } else null,
    )
}

@Composable
fun SourceAttribution(source: RefSource, modifier: Modifier = Modifier) =
    if (source == RefSource.TMDB) TmdbAttribution(modifier)
    else Text(
        stringResource(R.string.tvmaze_attribution), modifier.padding(16.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

/** Required TMDB notice, shown in About and wherever TMDB data appears. */
@Composable
fun TmdbAttribution(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.tmdb_attribution), modifier.padding(16.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
