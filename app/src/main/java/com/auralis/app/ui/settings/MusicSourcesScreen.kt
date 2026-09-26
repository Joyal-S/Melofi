package com.auralis.app.ui.settings

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyShapes
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.data.local.db.entity.ExcludedFolderEntity
import com.auralis.app.data.local.db.entity.ImportedFolderEntity
import kotlinx.coroutines.launch

/**
 * Music Sources & Excluded Folders management screen.
 * Allows users to review default audio source indexing and toggle or add custom
 * excluded directories (e.g., WhatsApp Audio, Voice Recorder, Call Recordings)
 * using the system Storage Access Framework without requiring MANAGE_EXTERNAL_STORAGE.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSourcesScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val importedFolders by viewModel.importedFolders.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var pendingRestoreFolder by remember { mutableStateOf<ImportedFolderEntity?>(null) }
    val restoreFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val target = pendingRestoreFolder
            if (target != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {}
                viewModel.updateFolderPermissionStatus(target.id, true)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Folder access restored: ${target.displayName}")
                }
            }
        }
        pendingRestoreFolder = null
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val (folderName, folderPath) = parseFolderDetails(uri)
            viewModel.addExcludedFolder(folderName, folderPath)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Excluded: $folderName ($folderPath)")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Music Sources & Folders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(MelofySpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MelofySpacing.lg)
        ) {
            // Primary Music Source Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MelofyShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MelofySpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(MelofySpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Main Music Library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Scans all device audio while strictly excluding voice notes, call recordings, ringtones, notifications, and podcasts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Imported Folders Section Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Imported Music Folders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline folders added directly via Storage Access Framework.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Imported Folders List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MelofyShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
            ) {
                Column(modifier = Modifier.padding(MelofySpacing.md)) {
                    if (importedFolders.isEmpty()) {
                        Text(
                            text = "No custom folders imported yet. Use Add Music -> Choose Folder to import directories.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(MelofySpacing.md)
                        )
                    } else {
                        importedFolders.forEachIndexed { index, folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = MelofySpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = if (folder.isPermissionValid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = if (folder.isPermissionValid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(MelofySpacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (folder.isPermissionValid) {
                                        Text(
                                            text = "${folder.songCount} songs indexed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "Folder access needs to be restored.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (!folder.isPermissionValid) {
                                    Button(
                                        onClick = {
                                            pendingRestoreFolder = folder
                                            restoreFolderLauncher.launch(Uri.parse(folder.treeUri))
                                        },
                                        contentPadding = ButtonDefaults.TextButtonContentPadding
                                    ) {
                                        Text("Restore Access", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteImportedFolder(folder.id) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = "Remove folder",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (index < importedFolders.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = MelofySpacing.xs),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Excluded Folders Section Header & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Excluded Folders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Files in these folders stay on your device but are excluded from Melofi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(MelofySpacing.sm))
                FilledTonalButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Add Folder", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Excluded Folders Card List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MelofyShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
            ) {
                Column(modifier = Modifier.padding(MelofySpacing.md)) {
                    if (excludedFolders.isEmpty()) {
                        Text(
                            text = "No folder exclusions active.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(MelofySpacing.md)
                        )
                    } else {
                        excludedFolders.forEachIndexed { index, folder ->
                            ExcludedFolderRow(
                                folder = folder,
                                onToggle = { isEnabled ->
                                    viewModel.toggleExcludedFolder(folder.id, isEnabled)
                                },
                                onDelete = if (!folder.isDefault) {
                                    { viewModel.deleteExcludedFolder(folder.id) }
                                } else null
                            )
                            if (index < excludedFolders.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = MelofySpacing.xs),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Tip / Guidance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MelofyShapes.card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.none)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MelofySpacing.lg),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(MelofySpacing.md))
                    Column {
                        Text(
                            text = "Safe & Offline Guarantee",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Excluding folders never deletes or alters your files. Non-music audio remains completely safe and untouched on your device storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExcludedFolderRow(
    folder: ExcludedFolderEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MelofySpacing.xs, horizontal = MelofySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = null,
                tint = if (folder.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(MelofySpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (folder.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = folder.folderPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (folder.isEnabled) 0.8f else 0.5f)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = folder.isEnabled,
                onCheckedChange = onToggle
            )
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(MelofySpacing.xs))
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Remove exclusion",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Extracts a readable folder name and trailing-slash folder path from a SAF DocumentTree Uri.
 */
private fun parseFolderDetails(treeUri: Uri): Pair<String, String> {
    val docId = try {
        DocumentsContract.getTreeDocumentId(treeUri)
    } catch (_: Exception) {
        treeUri.lastPathSegment ?: ""
    }

    val cleanPath = (if (docId.contains(":")) {
        docId.substringAfter(":")
    } else {
        docId
    }).trim('/')

    val folderName = cleanPath.substringAfterLast('/', cleanPath).ifBlank { "Custom Folder" }
    val folderPath = if (cleanPath.endsWith("/")) cleanPath else "$cleanPath/"
    return Pair(folderName, folderPath)
}
