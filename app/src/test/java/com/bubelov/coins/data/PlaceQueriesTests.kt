package com.bubelov.coins.data

import com.bubelov.coins.TestSuite
import org.junit.Test
import org.koin.core.inject
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

class PlaceQueriesTests : TestSuite() {

    private val queries: PlaceQueries by inject()

    @Test
    fun emptyByDefault() {
        assert(queries.selectCount().executeAsOne() == 0L)
    }

    @Test
    fun insertOrReplace_insertsItem() {
        val item = place()
        queries.insertOrReplace(item)

        assert(queries.selectCount().executeAsOne() == 1L)
        assert(queries.selectById(item.id).executeAsOne() == item)
    }

    @Test
    fun insertOrReplace_replacesItem() {
        val item = place()
        queries.insertOrReplace(item)

        val updatedItem = item.copy(name = "Changed")
        queries.insertOrReplace(updatedItem)

        assert(queries.selectCount().executeAsOne() == 1L)
        assert(queries.selectById(item.id).executeAsOne() == updatedItem)
    }

    @Test
    fun selectAll_selectsAllItems() {
        val items = listOf(place(), place())

        queries.transaction {
            items.forEach {
                queries.insertOrReplace(it)
            }
        }

        assert(queries.selectAll().executeAsList() == items)
    }

    @Test
    @ExperimentalStdlibApi
    fun selectById_selectsCorrectItem() {
        val items = buildList<Place> {
            repeat(100) {
                add(place())
            }
        }

        queries.transaction {
            items.forEach {
                queries.insertOrReplace(it)
            }
        }

        val randomItem = items.random()

        assert(queries.selectById(randomItem.id).executeAsOne() == randomItem)
    }

    @Test
    fun selectBySearchQuery_searchesByName() {
        queries.transaction {
            repeat(100) {
                var place = place()

                if (it == 1) {
                    place = place.copy(name = "Good Cafe")
                }

                if (it == 2) {
                    place = place.copy(name = "Bad Cafe")
                }

                queries.insertOrReplace(place)
            }
        }

        assert(queries.selectBySearchQuery("cafe").executeAsList().size == 2)
    }

    @Test
    fun selectRandom_returnsRandomItem() {
        val count = 1 + Random(System.currentTimeMillis()).nextInt(100)

        queries.transaction {
            repeat(count) {
                queries.insertOrReplace(place())
            }
        }

        var lastResult = queries.selectRandom().executeAsOneOrNull()

        while (queries.selectRandom().executeAsOne() == lastResult) {
            lastResult = queries.selectRandom().executeAsOneOrNull()
        }

        assert(true)
    }

    @Test
    fun selectCount_returnsCorrectCount() {
        val count = 1 + Random(System.currentTimeMillis()).nextInt(100)

        queries.transaction {
            repeat(count) {
                queries.insertOrReplace(place())
            }
        }

        assert(queries.selectCount().executeAsOne() == count.toLong())
    }

    @Test
    fun selectMaxUpdatedAt_selectsCorrectItem() {
        val count = 1 + Random(System.currentTimeMillis()).nextInt(100)

        queries.transaction {
            repeat(count) {
                queries.insertOrReplace(place())
            }
        }

        val item = place()
        val updatedAt = LocalDateTime.parse(item.updated_at).plusYears(5).toString()
        queries.insertOrReplace(item.copy(updated_at = updatedAt))

        assert(queries.selectMaxUpdatedAt().executeAsOne().MAX == updatedAt)
    }

    private fun place() = Place(
        id = UUID.randomUUID().toString(),
        source = "test",
        external_id = UUID.randomUUID().toString(),
        name = "",
        description = "",
        latitude = 0.0,
        longitude = 0.0,
        address = "",
        category = UUID.randomUUID().toString(),
        phone = "",
        website = "",
        opening_hours = "",
        valid = true,
        created_at = LocalDateTime.now().toString(),
        updated_at = LocalDateTime.now().toString()
    )
}