package com.huaMax.manager.ui.drawer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huaMax.BuildConfig
import com.huaMax.R
import com.huaMax.manager.ui.navigation.Screen
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.HeartSolid
import compose.icons.lineawesomeicons.InfoCircleSolid
import compose.icons.lineawesomeicons.MapMarkerAltSolid
import compose.icons.lineawesomeicons.MobileAltSolid
import compose.icons.lineawesomeicons.TelegramPlane

private object DrawerDimensions {
    val SECTION_SPACING = 14.dp
    val ITEM_SPACING = 5.dp
    val ICON_SIZE = 22.dp
    val SECTION_PADDING = 12.dp
    val HEADER_PADDING = 14.dp
    val DRAWER_PADDING = 14.dp
    val ITEM_HORIZONTAL_PADDING = 14.dp
    val ITEM_VERTICAL_PADDING = 10.dp
    val ITEM_CORNER_RADIUS = 16.dp
}

@Composable
fun DrawerContent(
    navController: NavController,
    onCloseDrawer: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 360.dp),
        drawerContainerColor = Color.Transparent,
        drawerContentColor = Color(0xFF061F1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 360.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF2FCFA),
                            Color(0xFFFFFFFF),
                            Color(0xFFEAF7F4)
                        )
                    )
                )
                .padding(DrawerDimensions.DRAWER_PADDING)
        ) {
            DrawerHeader()
            Spacer(modifier = Modifier.height(DrawerDimensions.SECTION_SPACING))
            DrawerSectionHeader(stringResource(R.string.drawer_navigation))

            DrawerItem(
                icon = LineAwesomeIcons.HeartSolid,
                label = stringResource(R.string.screen_favorites),
                onClick = {
                    navController.navigateFromDrawer(Screen.Favorites.route)
                    onCloseDrawer()
                },
                isSelected = navController.currentDestination?.route == Screen.Favorites.route
            )

            DrawerItem(
                icon = LineAwesomeIcons.MobileAltSolid,
                label = stringResource(R.string.screen_target_apps),
                onClick = {
                    navController.navigateFromDrawer(Screen.TargetApps.route)
                    onCloseDrawer()
                },
                isSelected = navController.currentDestination?.route == Screen.TargetApps.route
            )

            DrawerItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.screen_settings),
                onClick = {
                    navController.navigateFromDrawer(Screen.Settings.route)
                    onCloseDrawer()
                },
                isSelected = navController.currentDestination?.route == Screen.Settings.route
            )

            Spacer(modifier = Modifier.height(12.dp))
            DrawerSectionHeader(stringResource(R.string.drawer_community))

            DrawerItem(
                icon = LineAwesomeIcons.TelegramPlane,
                label = stringResource(R.string.drawer_telegram_group),
                onClick = {
                    uriHandler.openUri("https://t.me/+w4ftZ0ZAmrRhOTZl")
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            DrawerSectionHeader(stringResource(R.string.drawer_app_info))

            DrawerItem(
                icon = LineAwesomeIcons.InfoCircleSolid,
                label = stringResource(R.string.screen_about),
                onClick = {
                    navController.navigateFromDrawer(Screen.About.route)
                    onCloseDrawer()
                },
                isSelected = navController.currentDestination?.route == Screen.About.route
            )

            DrawerItem(
                icon = Icons.Default.SystemUpdate,
                label = stringResource(R.string.screen_update),
                onClick = {
                    navController.navigateFromDrawer(Screen.Update.route)
                    onCloseDrawer()
                },
                isSelected = navController.currentDestination?.route == Screen.Update.route
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.drawer_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A8581),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun NavController.navigateFromDrawer(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .shadow(7.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE9FAF6)
                    )
                )
            )
    ) {
        Icon(
            imageVector = LineAwesomeIcons.MapMarkerAltSolid,
            contentDescription = null,
            tint = Color(0xFF63CBB7).copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp)
                .size(58.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DrawerDimensions.HEADER_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(7.dp, RoundedCornerShape(16.dp), clip = false)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF04A88F),
                                Color(0xFF007A67)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LineAwesomeIcons.MapMarkerAltSolid,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF061F1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.drawer_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F6B68),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Column(
        modifier = Modifier.padding(
            start = DrawerDimensions.SECTION_PADDING,
            bottom = 6.dp
        )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007B69)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF007B69), Color(0xFFB9EFE3))
                    )
                )
        )
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val itemBorder = if (isSelected) {
        BorderStroke(1.dp, Color(0xFFBDEFE4))
    } else {
        null
    }

    val contentColor = if (isSelected) {
        Color(0xFF061F1A)
    } else {
        Color(0xFF43514D)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DrawerDimensions.ITEM_SPACING)
            .shadow(4.dp, RoundedCornerShape(DrawerDimensions.ITEM_CORNER_RADIUS), clip = false)
            .clip(RoundedCornerShape(DrawerDimensions.ITEM_CORNER_RADIUS))
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFF0FFFB) else Color.White.copy(alpha = 0.96f),
        border = itemBorder,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DrawerDimensions.ITEM_HORIZONTAL_PADDING,
                    vertical = DrawerDimensions.ITEM_VERTICAL_PADDING
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF87E7D3),
                                    Color(0xFFDFF9F3)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFF2FBF9),
                                    Color(0xFFFFFFFF)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(DrawerDimensions.ICON_SIZE),
                    tint = if (isSelected) Color(0xFF007B69) else Color(0xFF294A45)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            trailingIcon?.invoke() ?: Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF06483E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
