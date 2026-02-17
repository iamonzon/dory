package com.iamonzon.dory.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.iamonzon.dory.R
import com.iamonzon.dory.ui.theme.DoryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoryTopAppBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_navigate_back)
                    )
                }
            }
        },
        actions = { actions() },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DoryTopAppBarPreview() {
    DoryTheme {
        DoryTopAppBar(title = "Dory", onBackClick = {})
    }
}
