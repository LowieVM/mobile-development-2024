package com.example.rentify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rentify.database.FirebaseAuthManager
import com.example.rentify.ui.theme.RentifyTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Properties


class RegisterActivity : ComponentActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private var locationIqApiKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationIqApiKey = loadApiKey()

        firebaseAuthManager = FirebaseAuthManager(this)

        setContent {
            RentifyTheme {
                RegisterScreen(
                    onRegister = { username, email, password, address, lat, lon ->
                        firebaseAuthManager.registerUser(username, email, password, address, lat, lon) { success ->
                            if (success) {
                                finish()
                            }
                        }
                    },
                    onBackPress = { finish() },
                    apiKey = locationIqApiKey
                )
            }
        }
    }

    private fun loadApiKey(): String {
        return try {
            val properties = Properties()
            val inputStream = assets.open("secrets.properties")
            properties.load(inputStream)
            properties.getProperty("LOCATION_IQ_API_KEY") ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegister: (username: String, email: String, password: String, address: String, lat: String, lon: String) -> Unit,
    onBackPress: () -> Unit,
    apiKey: String
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<LocationSuggestion>()) }

    val isFormValid = username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && address.isNotEmpty() && lat.isNotEmpty() && lon.isNotEmpty()

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

            // Address Field with Suggestions
            TextField(
                value = address,
                onValueChange = { query ->
                    address = query
                    if (query.isNotEmpty()) {
                        fetchLocationSuggestions(
                            query = query,
                            apiKey = apiKey,
                            onSuccess = { suggestions = it },
                            onError = { suggestions = emptyList() }
                        )
                    } else {
                        suggestions = emptyList()
                    }
                },
                label = { Text("Address") }
            )

            // Suggestions Dropdown
            LazyColumn {
                items(suggestions) { suggestion ->
                    Text(
                        text = suggestion.display_name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                address = suggestion.display_name
                                lat = suggestion.lat
                                lon = suggestion.lon
                                suggestions = emptyList()  // Clear suggestions on selection
                            }
                    )
                }
            }

            if (!isFormValid) {
                Text(
                    text = "Please fill in all fields.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        onRegister(username, email, password, address, lat, lon)
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

data class LocationSuggestion(
    val place_id: String,
    val lat: String,
    val lon: String,
    val display_name: String
)


fun fetchLocationSuggestions(
    query: String,
    apiKey: String,
    onSuccess: (List<LocationSuggestion>) -> Unit,
    onError: (Exception) -> Unit
) {
    val client = OkHttpClient()
    val url = "https://us1.locationiq.com/v1/autocomplete?key=$apiKey&q=$query&limit=3"

    val request = Request.Builder()
        .url(url)
        .get()
        .addHeader("accept", "application/json")
        .build()

    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                val type = object : TypeToken<List<LocationSuggestion>>() {}.type
                val suggestions: List<LocationSuggestion> = Gson().fromJson(body, type)
                onSuccess(suggestions)
            } else {
                onError(Exception("Failed to fetch suggestions: ${response.message}"))
            }
        } catch (e: Exception) {
            onError(e)
        }
    }.start()
}







@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RentifyTheme {
        RegisterScreen(
            onRegister = { _, _, _, _, _, _ -> },
            onBackPress = { },
            apiKey = ""
        )
    }
}

