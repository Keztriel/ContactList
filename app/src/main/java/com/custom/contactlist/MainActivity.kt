package com.custom.contactlist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Cursor.getStringOrNull(columnIndex: Int): String? =
    if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex) else null

data class Contact(val name: String?, val phoneNumber: String?, val email: String?)

@Suppress("Range")
fun Context.fetchAllContacts(): List<Contact> {
    Log.d("FETCH", "fetchAllContacts called")
    return contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    )?.use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val name = cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber = cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val email = cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
                add(Contact(name, phoneNumber, email))
            }
        }
    } ?: emptyList()
}

@Composable
fun ContactsScreen() {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var contacts by remember { mutableStateOf<List<Contact>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            contacts = withContext(Dispatchers.IO) { context.fetchAllContacts() }
            isLoading = false
        } else {
            contacts = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            !hasPermission -> {
                PermissionDeniedView {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            }
            isLoading -> {
                CircularProgressIndicator()
            }
            contacts != null -> {
                ContactsList(contacts = contacts!!)
            }
        }
    }
}

@Composable
fun ContactsList(contacts: List<Contact>) {
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (contacts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(0.8f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.no_contacts_found))
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.contacts_count, contacts.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            items(contacts) { contact ->
                ContactListItem(contact = contact) {
                    contact.phoneNumber?.let { phone ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        context.startActivity(intent)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}


@Composable
fun ContactListItem(contact: Contact, onClick: () -> Unit) {
    Text(
        text = contact.name ?: stringResource(R.string.unknown_contact),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}


@Composable
fun RequestPermissionView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.permission_required_message))
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.request_permission))
        }
    }
}

@Composable
fun PermissionDeniedView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.permission_denied_message))
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.request_permission_again))
        }
    }
}


@Composable
fun CustomAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            background = Color.White,
            onBackground = Color.Black,
            primary = Color(0xFF6200EE),
            onPrimary = Color.White
        ),
        content = content
    )
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.White.toArgb()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            CustomAppTheme {
                Scaffold { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        ContactsScreen()
                    }
                }
            }
        }
    }
}