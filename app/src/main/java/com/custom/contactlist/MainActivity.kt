package com.custom.contactlist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
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

    var showPermissionRationale by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<Contact>?>(null) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        showPermissionRationale = !granted
    }


    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = withContext(Dispatchers.IO) { context.fetchAllContacts() }
            showPermissionRationale = false
        } else {
            contacts = null
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                !hasPermission -> {
                    if (showPermissionRationale) {
                        PermissionDeniedView { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    } else {
                        RequestPermissionView { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    }
                }
                contacts == null -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                contacts.isNullOrEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_contacts_found),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    ContactsList(contacts = contacts!!, onContactClick = { selectedContact = it })
                }
            }

            selectedContact?.let {
                ContactDetailsDialog(contact = it, onDismiss = { selectedContact = null })
            }
        }
    }
}

@Composable
fun ContactsList(contacts: List<Contact>, onContactClick: (Contact) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.contact_list_header),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
//                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        items(contacts) { contact ->
            ContactListItem(contact = contact, onClick = { onContactClick(contact) })
            Divider()
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
            .padding(16.dp)
    )
}

@Composable
fun ContactDetailsDialog(contact: Contact, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        title = {
            Text(text = contact.name ?: stringResource(R.string.unknown_contact))
        },
        text = {
            Column {
                Text("${stringResource(R.string.phone_number)}: ${contact.phoneNumber ?: "-"}")
                Text("${stringResource(R.string.email)}: ${contact.email ?: "-"}")
            }
        }
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