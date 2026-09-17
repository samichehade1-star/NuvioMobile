package com.nuvio.app.features.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.home.components.HomeCatalogRowSection
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.home.components.homeSectionHorizontalPaddingForWidth
import com.nuvio.app.features.watched.WatchedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.networks_hub_empty_message
import nuvio.composeapp.generated.resources.networks_hub_empty_title
import nuvio.composeapp.generated.resources.networks_hub_title
import org.jetbrains.compose.resources.stringResource

/**
 * The "browse by streaming service" hub: Netflix, Disney+, Prime Video, etc., each as a real
 * row of titles (not a click-through folder tile). Reuses the same network collections and
 * [HomeCatalogSection] machinery Home uses for its rows, including "See All"/pagination.
 */
@Composable
fun NetworksHubScreen(
    modifier: Modifier = Modifier,
    topChromePadding: Dp? = null,
    listState: LazyListState = rememberLazyListState(),
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    var sections by remember { mutableStateOf<List<HomeCatalogSection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        CollectionRepository.initialize()
        isLoading = true
        sections = fetchNetworkHomeSections()
        isLoading = false
    }

    ScreenActivityEffect(scrollToTopRequests) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            listState.animateScrollToItem(0)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val homeSectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value)
        NuvioScreen(
            horizontalPadding = 0.dp,
            topPadding = if (topChromePadding != null) 0.dp else null,
            listState = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                NuvioScreenHeader(
                    title = stringResource(Res.string.networks_hub_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    topPadding = topChromePadding,
                )
            }
            when {
                isLoading && sections.isEmpty() -> {
                    items(3) {
                        HomeSkeletonRow(horizontalPadding = homeSectionPadding)
                    }
                }

                sections.isEmpty() -> {
                    item {
                        HomeEmptyStateCard(
                            modifier = Modifier.padding(horizontal = homeSectionPadding),
                            title = stringResource(Res.string.networks_hub_empty_title),
                            message = stringResource(Res.string.networks_hub_empty_message),
                        )
                    }
                }

                else -> {
                    items(
                        items = sections,
                        key = { section -> section.key },
                    ) { section ->
                        HomeCatalogRowSection(
                            section = section,
                            modifier = Modifier.padding(bottom = 12.dp),
                            watchedKeys = watchedUiState.watchedKeys,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            onViewAllClick = onCatalogClick?.let { { it(section) } },
                            onPosterClick = onPosterClick,
                            onPosterLongClick = onPosterLongClick,
                        )
                    }
                }
            }
        }
    }
}
