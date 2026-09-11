package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive JUnit DAO Test Suite for Room Database.
 * Verifies data integrity for menu items, order history, and offline queue caching layer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
    fun testFoodItemInsertionAndRetrieval() = runBlocking {
        val vendor = User(
            id = 1,
            username = "vendor_test",
            passwordHash = "hash123",
            role = "VENDOR",
            fullName = "Vendor One",
            info = "Stall 1"
        )
        userDao.insertUser(vendor)

        val item = FoodItem(
            id = 101,
            vendorId = 1,
            name = "Waakye Special with Fish & Egg",
            price = 35.00,
            category = "Main Meal",
            imageUrl = "https://example.com/waakye.jpg",
            description = "Authentic Ghanaian waakye served with shito, boiled egg, and fried fish",
            isAvailable = true,
            initialStock = 50,
            currentStock = 50
        )

        foodItemDao.insertFoodItem(item)
        val items = foodItemDao.getAllFoodItems().first()

        assertNotNull(items)
        assertEquals(1, items.size)
        val retrieved = items[0]
        assertEquals("Waakye Special with Fish & Egg", retrieved.name)
        assertEquals(35.00, retrieved.price, 0.001)
        assertEquals("Main Meal", retrieved.category)
        assertTrue(retrieved.isAvailable)
        assertEquals(50, retrieved.currentStock)
    }

    @Test
    fun testOrderCreationAndOrderHistoryQuery() = runBlocking {
        val student = User(
            id = 99,
            username = "student_99",
            passwordHash = "hash123",
            role = "STUDENT",
            fullName = "Student 99",
            info = "STU-2026-99"
        )
        val vendor = User(
            id = 5,
            username = "vendor_5",
            passwordHash = "hash123",
            role = "VENDOR",
            fullName = "Vendor Five",
            info = "Stall 5"
        )
        userDao.insertUser(student)
        userDao.insertUser(vendor)

        val order1 = Order(
            id = 1001,
            customerId = 99,
            vendorId = 5,
            foodItemId = 101,
            foodName = "Jollof Rice with Chicken",
            quantity = 2,
            unitPrice = 25.00,
            totalPrice = 50.00,
            status = "PLACED",
            pickupPin = "1234"
        )
        val order2 = Order(
            id = 1002,
            customerId = 99,
            vendorId = 5,
            foodItemId = 102,
            foodName = "Fried Plantain & Beans (Gob3)",
            quantity = 1,
            unitPrice = 20.00,
            totalPrice = 20.00,
            status = "COMPLETED",
            pickupPin = "5678"
        )

        orderDao.insertOrder(order1)
        orderDao.insertOrder(order2)

        val allOrders = orderDao.getAllOrders().first()
        assertEquals(2, allOrders.size)

        val studentOrders = orderDao.getOrdersForCustomer(99).first()
        assertEquals(2, studentOrders.size)
    }

    @Test
    fun testOfflineOrderQueuePersistence() = runBlocking {
        val offlineOrder = OfflineOrder(
            id = 501,
            customerId = 99,
            vendorId = 5,
            foodItemId = 101,
            foodName = "Koko & Koose",
            quantity = 2,
            unitPrice = 6.25,
            totalPrice = 12.50
        )

        offlineOrderDao.insertOfflineOrder(offlineOrder)

        val pendingQueue = offlineOrderDao.getAllOfflineOrders()
        assertEquals(1, pendingQueue.size)
        assertEquals("Koko & Koose", pendingQueue[0].foodName)
        assertEquals(12.50, pendingQueue[0].totalPrice, 0.001)

        // Simulate syncing and clearing queue
        offlineOrderDao.deleteOfflineOrder(pendingQueue[0])
        val emptyQueue = offlineOrderDao.getAllOfflineOrders()
        assertTrue(emptyQueue.isEmpty())
    }

    @Test
    fun testUserProfileCachingAndRetrieval() = runBlocking {
        val user = User(
            id = 1,
            username = "kwame_atu",
            passwordHash = "secure_hash",
            fullName = "Kwame Mensah",
            role = "STUDENT",
            email = "kwame.mensah@atu.edu.gh",
            telephone = "+233241234567",
            info = "INDEX: 012345678"
        )

        userDao.insertUser(user)
        val cachedUser = userDao.getUserByUsername("kwame_atu")

        assertNotNull(cachedUser)
        assertEquals("Kwame Mensah", cachedUser?.fullName)
        assertEquals("kwame.mensah@atu.edu.gh", cachedUser?.email)
    }
}
