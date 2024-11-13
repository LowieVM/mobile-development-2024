package com.example.rentify


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rentify.add.AddScreen
import com.example.rentify.add.AddViewModel
import com.example.rentify.add.RentScreen
import com.example.rentify.add.RentViewModel
import com.example.rentify.profile.ProfileScreen
import com.example.rentify.profile.ProfileViewModel
import com.example.rentify.ui.theme.RentifyTheme
import dagger.hilt.android.AndroidEntryPoint

data class TabBarItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeAmount: Int? = null
)

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val rentViewModel: RentViewModel by viewModels()
    private val addViewModel: AddViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // setting up the individual tabs
            val rentTab = TabBarItem(title = "Rent", selectedIcon = Icons.Filled.LocationOn, unselectedIcon = Icons.Outlined.LocationOn)
            val addTab = TabBarItem(title = "Add", selectedIcon = Icons.Filled.Add, unselectedIcon = Icons.Outlined.Add)
            val profileTab = TabBarItem(title = "Profile", selectedIcon = Icons.Filled.Person, unselectedIcon = Icons.Outlined.Person, badgeAmount = 2)

            // creating a list of all the tabs
            val tabBarItems = listOf(rentTab, addTab, profileTab)

            // creating our navController
            val navController = rememberNavController()

            RentifyTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(bottomBar = { TabView(tabBarItems, navController) }) {
                        NavHost(navController = navController, startDestination = rentTab.title) {
                            composable(rentTab.title) {
                                val userName by rentViewModel.userName.observeAsState("User")
                                RentScreen(userName = userName)
                            }
                            composable(addTab.title) {
                                val userName by profileViewModel.userName.observeAsState("User")
                                AddScreen(userName = userName)
                            }
                            composable(profileTab.title) {
                                val userName by profileViewModel.userName.observeAsState("User")
                                ProfileScreen(userName = userName, onLogoutClicked = {
                                    profileViewModel.logoutUser()
                                    val intent = Intent(this@HomeActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// This is a wrapper view that allows us to easily and cleanly
// reuse this component in any future project
@Composable
fun TabView(tabBarItems: List<TabBarItem>, navController: NavController) {
    var selectedTabIndex by rememberSaveable {
        mutableStateOf(0)
    }

    NavigationBar {
        // looping over each tab to generate the views and navigation for each item
        tabBarItems.forEachIndexed { index, tabBarItem ->
            NavigationBarItem(
                selected = selectedTabIndex == index,
                onClick = {
                    selectedTabIndex = index
                    navController.navigate(tabBarItem.title)
                },
                icon = {
                    TabBarIconView(
                        isSelected = selectedTabIndex == index,
                        selectedIcon = tabBarItem.selectedIcon,
                        unselectedIcon = tabBarItem.unselectedIcon,
                        title = tabBarItem.title,
                        badgeAmount = tabBarItem.badgeAmount
                    )
                },
                label = {Text(tabBarItem.title)})
        }
    }
}

// This component helps to clean up the API call from our TabView above,
// but could just as easily be added inside the TabView without creating this custom component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabBarIconView(
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    title: String,
    badgeAmount: Int? = null
) {
    BadgedBox(badge = { TabBarBadgeView(badgeAmount) }) {
        Icon(
            imageVector = if (isSelected) {selectedIcon} else {unselectedIcon},
            contentDescription = title
        )
    }
}

// This component helps to clean up the API call from our TabBarIconView above,
// but could just as easily be added inside the TabBarIconView without creating this custom component
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TabBarBadgeView(count: Int? = null) {
    if (count != null) {
        Badge {
            Text(count.toString())
        }
    }
}
// end of the reusable components that can be copied over to any new projects
// ----------------------------------------