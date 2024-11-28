package com.example.rentify.rent

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rentify.R
import com.example.rentify.shared.ItemCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import kotlin.math.ln

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RentScreen(viewModel: RentViewModel = viewModel(), context: Context) {
    val items = viewModel.items.observeAsState(initial = emptyList())
    val state by viewModel.state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = viewModel::fetchItems
    )
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("All Categories", "Bouwgereedschap", "Keukenapparatuur", "Schoonmaakapparatuur", "Transportbenodigdheden", "Tuinbenodigdheden")
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var searchQuery by remember { mutableStateOf("") }
    var radiusInKm by remember { mutableFloatStateOf(30f) } // Default radius

    var isMapVisible by remember { mutableStateOf(false) } // State to track map visibility


    val currentUser = FirebaseAuth.getInstance().currentUser
    val userLocation by viewModel.userLocation.observeAsState(GeoPoint(50.0, 4.0))
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { viewModel.fetchUserLocation(it) }
    }


    val centerGeoPoint = userLocation

    val filteredItems = items.value.filter { item ->
        val itemCategory = item["category"] as? String ?: ""
        val itemName = item["itemName"] as? String ?: ""
        val itemLat = item["latitude"] as? String ?: "0.0"
        val itemLong = item["longitude"] as? String ?: "0.0"
        val itemGeoPoint = GeoPoint(itemLat.toDouble(), itemLong.toDouble())

        println(itemName)
        println(itemLat)
        println(itemLong)

        val isInRadius = itemGeoPoint.distanceToAsDouble(centerGeoPoint) <= radiusInKm * 1000
        (selectedCategory == "All Categories" || itemCategory == selectedCategory) &&
                itemName.contains(searchQuery, ignoreCase = true) &&
                isInRadius
    }

    fun resizeDrawable(context: Context, drawableResId: Int, width: Int, height: Int): Drawable? {
        val originalDrawable = ContextCompat.getDrawable(context, drawableResId) ?: return null
        val bitmap = (originalDrawable as BitmapDrawable).bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return BitmapDrawable(context.resources, scaledBitmap)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rent") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search items") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // Radius slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Distance: ${radiusInKm.toInt()} km", modifier = Modifier.weight(1f))

                        // Add the IconButton to toggle map visibility
                        androidx.compose.material3.IconButton(
                            onClick = { isMapVisible = !isMapVisible }
                        ) {
                            Icon(
                                imageVector = if (isMapVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isMapVisible) "Hide Map" else "Show Map"
                            )
                        }
                    }
                    if (isMapVisible) {
                        androidx.compose.material3.Slider(
                            value = radiusInKm,
                            onValueChange = {
                                radiusInKm = it
                            },
                            valueRange = 1f..150f, // 1 km to 50 km
                            steps = 149 // Optional, 1 step per km
                        )
                    }
                }

                if (isMapVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(8.dp)
                            .graphicsLayer { clip = true }
                    ) {
                        AndroidView(
                            factory = { context ->
                                // Create the MapView
                                MapView(context).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                                    // Center the map on a default location
                                    controller.setZoom(10.0)
                                    controller.setCenter(centerGeoPoint)
                                }
                            },
                            update = { mapView ->
                                // Ensure markers are added when the MapView is ready
                                mapView.apply {
                                    // Clear existing markers to avoid duplicates on re-render
                                    overlays.clear()
                                    overlays.add(CircleOverlay(centerGeoPoint, radiusInKm * 1000.0)) // Re-add the circle overlay
                                    controller.setZoom(-1.4412287385975584 * ln(40.46204143949824 * radiusInKm) + 20.31274055274244)
                                    controller.setCenter(centerGeoPoint)

                                    val smallMarkerPinIcon = resizeDrawable(context, R.drawable.red_marker, 24, 24)
                                    val smallMarkerHomeIcon = resizeDrawable(context, R.drawable.cyan_home, 20, 20)

                                    val homeMarker = Marker(this)
                                    homeMarker.position = centerGeoPoint
                                    homeMarker.title = "Home"
                                    homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                                    homeMarker.icon = smallMarkerHomeIcon

                                    overlays.add(homeMarker) // Add marker to the map

                                    filteredItems.groupBy { item ->
                                        val lat = (item["latitude"] as? String ?: "0.0").toDouble()
                                        val lon = (item["longitude"] as? String ?: "0.0").toDouble()
                                        GeoPoint(lat, lon)
                                    }.forEach { (location, itemsAtLocation) ->
                                        if (location == centerGeoPoint) {
                                            return@forEach
                                        }
                                        val marker = Marker(mapView)
                                        marker.position = location
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        marker.icon = smallMarkerPinIcon

                                        val title = itemsAtLocation.joinToString(separator = "\n") { item ->
                                            item["itemName"] as? String ?: "Unknown Item"
                                        }
                                        marker.title = title
                                        overlays.add(marker)
                                    }
                                    invalidate() // Trigger a redraw of the map
                                }
                            }
                        )
                    }
                }



                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDropdownExpanded = !isDropdownExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start,
                            color = if (selectedCategory.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isDropdownExpanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isDropdownExpanded) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp), // Fixed height for dropdown
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            LazyColumn {
                                items(categories.size) { index ->
                                    val category = categories[index]
                                    Text(
                                        text = category,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCategory = category
                                                isDropdownExpanded = false
                                            }
                                            .padding(8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Allow the grid to take most of the space
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems.size) { index ->
                        val item = filteredItems[index]
                        val ownerRef = item["userRef"] as? DocumentReference

                        val ownerId = ownerRef?.id
                        println(ownerId)

                        ItemCard(item = item, onClick = {
                            // Start the activity and pass the item data
                            val intent = Intent(context, RentItemActivity::class.java)
                            intent.putExtra("itemName", item["itemName"] as? String ?: "")
                            intent.putExtra("itemPrice", item["itemPrice"] as? String ?: "")
                            intent.putExtra("itemDescription", item["itemDescription"] as? String ?: "")
                            intent.putExtra("imageUrl", item["imageUrl"] as? String ?: "")
                            intent.putExtra("documentId", item["documentId"] as? String ?: "")

                            // Pass the ownerId to the RentItemActivity
                            intent.putExtra("ownerId", ownerId ?: "") // Default to empty if ownerId is null
                            context.startActivity(intent)
                        })
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Adjust the height as needed
            }

            PullRefreshIndicator(
                refreshing = viewModel.state.value.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}


class CircleOverlay(
    private val center: GeoPoint,
    private val radiusInMeters: Double
) : Overlay() {
    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        strokeWidth = 1f
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLUE
        alpha = 25 // Adjust alpha (0-255) for transparency; 50 is semi-transparent
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return // Skip shadow layers

        val projection = mapView.projection

        // Convert the center GeoPoint to screen pixels
        val screenPoint = Point()
        projection.toPixels(center, screenPoint)

        // Compute the pixel radius based on the radiusInMeters
        val groundResolution = projection.metersToPixels(radiusInMeters.toFloat())

        // Draw the filled circle
        canvas.drawCircle(
            screenPoint.x.toFloat(),
            screenPoint.y.toFloat(),
            groundResolution,
            fillPaint
        )

        // Draw the circle outline
        canvas.drawCircle(
            screenPoint.x.toFloat(),
            screenPoint.y.toFloat(),
            groundResolution,
            strokePaint
        )
    }
}
