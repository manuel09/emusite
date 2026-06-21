package com.emusite.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.tvFocusable(focusRequester: FocusRequester? = null): Modifier {
    val requester = focusRequester ?: remember { FocusRequester() }
    return this
        .focusRequester(requester)
        .focusable()
        .padding(4.dp)
        .border(
            BorderStroke(0.dp, MaterialTheme.colorScheme.surface)
        )
}
