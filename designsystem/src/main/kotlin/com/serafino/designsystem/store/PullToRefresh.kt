package com.serafino.designsystem.store

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.Theme

/**
 * Pull-to-refresh de la app: el control NATIVO de Material 3 ([PullToRefreshBox]) con el
 * indicador en la paleta Serafino (cápsula espresso + spinner dorado). Aplicar alrededor
 * del contenido scrolleable. [indicatorTopPadding] baja el indicador para que no quede
 * bajo la barra flotante cuando el scroll sangra edge-to-edge (el hero de la Tienda).
 * Es el análogo del `pullToRefresh` propio de iOS — acá el gesto nativo sí funciona,
 * así que solo personalizamos la estética.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerafinoPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorTopPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = indicatorTopPadding),
                containerColor = Theme.Palette.espresso,
                color = Theme.Palette.gold,
            )
        },
        content = content,
    )
}
