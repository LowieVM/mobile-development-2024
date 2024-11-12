package com.example.rentify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rentify.database.FirebaseAuthManager
import com.example.rentify.ui.theme.RentifyTheme
import com.example.rentify.utils.LocationHelper

class RegisterActivity : ComponentActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private lateinit var locationHelper: LocationHelper
    private var userLocation by mutableStateOf("Unknown")

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchLocation()
            } else {
                Toast.makeText(this@RegisterActivity, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    private fun fetchLocation() {
        locationHelper.getLastKnownLocation { location ->
            if (location != null) {
                userLocation = location
            } else {
                Toast.makeText(this@RegisterActivity, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuthManager = FirebaseAuthManager(this)
        locationHelper = LocationHelper(this)

        setContent {
            RentifyTheme {
                RegisterScreen(
                    location = userLocation,
                    onRegister = { username, email, password, location ->
                        firebaseAuthManager.registerUser(username, email, password, location) { success ->
                            if (success) {
                                finish()
                            }
                        }
                    },
                    onBackPress = { finish() },
                    onFetchLocation = {
                        if (locationHelper.checkLocationPermission()) {
                            fetchLocation()
                        } else {
                            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                )
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    location: String,
    onRegister: (username: String, email: String, password: String, location: String) -> Unit,
    onBackPress: () -> Unit,
    onFetchLocation: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var locationState by remember { mutableStateOf(location) }

    LaunchedEffect(location) {
        locationState = location
    }

    val isFormValid = username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && locationState != "Unknown"

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        TopAppBar(
            title = { Text("Register") },
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = locationState,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onFetchLocation() }) {
                    Text("Get Location", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (!isFormValid) {
                Text(
                    text = "Please fill in all fields and get a valid location.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        onRegister(username, email, password, locationState)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) {
                Text("Register")
            }
        }
    }
}




@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RentifyTheme {
        RegisterScreen(
            location = "51.229955, 4.416175",
            onRegister = { _, _, _, _ -> },
            onBackPress = {  },
            onFetchLocation = {  }
        )
    }
}

