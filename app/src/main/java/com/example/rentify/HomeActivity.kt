package com.example.rentify


import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rentify.add.AddScreen
import com.example.rentify.add.AddViewModel
import com.example.rentify.profile.MyItemsActivity
import com.example.rentify.profile.ProfileScreen
import com.example.rentify.profile.ProfileViewModel
import com.example.rentify.profile.RentedItemsActivity
import com.example.rentify.rent.RentScreen
import com.example.rentify.rent.RentViewModel
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

    private val context: Context = this
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
                                RentScreen(viewModel = rentViewModel, context = context)
                            }
                            composable(addTab.title) {
                                AddScreen(onAddItem = { itemName, itemDescription, itemPrice, category, imageUri ->
                                    addViewModel.addItem(itemName, itemDescription, itemPrice, category, imageUri)
                                })
                            }
                            composable(profileTab.title) {
                                ProfileScreen(onLogoutClicked = {
                                    profileViewModel.logoutUser()
                                    val intent = Intent(this@HomeActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }, onRentedItemsClicked = {
                                    val intent = Intent(context, RentedItemsActivity::class.java)
                                    startActivity(intent)
                                }, onYourItemsClicked = {
                                    val intent = Intent(context, MyItemsActivity::class.java)
                                    startActivity(intent)
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
    // Observing the current destination from NavController
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedTabIndex = tabBarItems.indexOfFirst { it.title == currentDestination }

    NavigationBar {
        tabBarItems.forEachIndexed { index, tabBarItem ->
            NavigationBarItem(
                selected = selectedTabIndex == index,
                onClick = {
                    if (selectedTabIndex != index) {
                        navController.navigate(tabBarItem.title) {
                            // Ensure only a single instance of a destination exists in the back stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
                label = { Text(tabBarItem.title) }
            )
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