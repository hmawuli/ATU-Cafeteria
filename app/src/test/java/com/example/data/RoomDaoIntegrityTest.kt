package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive JUnit 5 DAO Test Suite for Room Database.
 * Verifies data integrity for menu items, order history, and offline queue caching layer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDaoIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var foodItemDao: FoodItemDao
    private lateinit var orderDao: OrderDao
    private lateinit var offlineOrderDao: OfflineOrderDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        foodItemDao = db.foodItemDao()
        orderDao = db.orderDao()
        offlineOrderDao = db.offlineOrderDao()
        userDao = db.userDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test FoodItem insertion and retrieval maintains data integrity`() = runBlocking {
        val item = FoodItem(
            id = 101,
            name = "Waakye Special with Fish & Egg",
            price = 35.00,
            category = "Main Meal",
            imageUrl = "https://example.com/waakye.jpg",
            description = "Authentic Ghanaian waakye served with shito, boiled egg, and fried fish",
            prepTimeMinutes = 15,
            isAvailable = true,
            stockQuantity = 50
        )

        foodItemDao.insertFoodItem(item)
        val items = foodItemDao.getAllFoodItems()

        assertNotNull(items)
        assertEquals(1, items.size)
        val retrieved = items[0]
        assertEquals("Waakye Special with Fish & Egg", retrieved.name)
        assertEquals(35.00, retrieved.price, 0.001)
        assertEquals("Main Meal", retrieved.category)
        assertTrue(retrieved.isAvailable)
        assertEquals(50, retrieved.stockQuantity)
    }

    @Test
    fun `test Order creation and order history query`() = runBlocking {
        val order1 = Order(
            id = 1001,
            foodName = "Jollof Rice with Chicken",
            quantity = 2,
            totalPrice = 50.00,
            vendorName = "Auntie Mary Jollof Spot",
            vendorId = 5,
            status = "PLACED",
            customerId = "STU-2026-99"
        )
        val order2 = Order(
            id = 1002,
            foodName = "Fried Plantain & Beans (Gob3)",
            quantity = 1,
            totalPrice = 20.00,
            vendorName = "Kojo Fast Foods",
            vendorId = 8,
            status = "COMPLETED",
            customerId = "STU-2026-99"
        )

        orderDao.insertOrder(order1)
        orderDao.insertOrder(order2)

        val allOrders = orderDao.getAllOrders()
        assertEquals(2, allOrders.size)

        // Verify ordering or content
        val studentOrders = orderDao.getOrdersByCustomerId("STU-2026-99")
        assertEquals(2, studentOrders.size)
    }

    @Test
    fun `test OfflineOrder queue persistence and clearance for offline caching layer`() = runBlocking {
        val offlineOrder = OfflineOrder(
            id = 501,
            foodId = 101,
            foodName = "Koko & Koose",
            price = 12.50,
            quantity = 2,
            vendorId = 2,
            customerId = "STU-OFFLINE-01",
            createdAt = System.currentTimeMillis()
        )

        offlineOrderDao.insertOfflineOrder(offlineOrder)

        val pendingQueue = offlineOrderDao.getAllOfflineOrders()
        assertEquals(1, pendingQueue.size)
        assertEquals("Koko & Koose", pendingQueue[0].foodName)
        assertEquals(12.50, pendingQueue[0].price, 0.001)

        // Simulate syncing and clearing queue
        offlineOrderDao.deleteOfflineOrder(pendingQueue[0])
        val emptyQueue = offlineOrderDao.getAllOfflineOrders()
        assertTrue(emptyQueue.isEmpty())
    }

    @Test
    fun `test User profile caching and retrieval`() = runBlocking {
        val user = User(
            id = 1,
            username = "kwame_atu",
            fullName = "Kwame Mensah",
            role = "STUDENT",
            email = "kwame.mensah@atu.edu.gh",
            phoneNumber = "+233241234567",
            info = "INDEX: 012345678"
        )

        userDao.insertUser(user)
        val cachedUser = userDao.getUserByUsername("kwame_atu")

        assertNotNull(cachedUser)
        assertEquals("Kwame Mensah", cachedUser?.fullName)
        assertEquals("kwame.mensah@atu.edu.gh", cachedUser?.email)
    }
}
