package com.recipetracker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.recipetracker.data.PhotoStorage
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.db.IterationEntity
import com.recipetracker.data.db.PhotoEntity
import com.recipetracker.data.db.RecipeEntity
import java.time.LocalDate
import kotlin.random.Random

/** Fills a repository with believable recipes and simple generated "food photos". */
object SampleData {
    fun fakePhoto(seed: Int, top: Int, bottom: Int): Bitmap {
        val rnd = Random(seed)
        val bmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, 800f, 600f, Paint().apply { shader = LinearGradient(0f, 0f, 0f, 600f, top, bottom, Shader.TileMode.CLAMP) })
        // a plate
        c.drawCircle(400f, 320f, 230f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F1EC.toInt() })
        c.drawCircle(400f, 320f, 180f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(400f, 320f, 180f, 0xFFE8B04A.toInt(), 0xFFB5532F.toInt(), Shader.TileMode.CLAMP)
        })
        repeat(14) {
            c.drawCircle(300f + rnd.nextFloat() * 200, 220f + rnd.nextFloat() * 200, 10f + rnd.nextFloat() * 18,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = listOf(0xFF6B8E23, 0xFFC0392B, 0xFFF5E6C4, 0xFF3E2723).random(rnd).toInt() })
        }
        return bmp
    }

    suspend fun populate(repo: RecipeRepository, photos: PhotoStorage, withPhotos: Boolean = true) {
        val now = System.currentTimeMillis()
        val today = LocalDate.of(2026, 11, 22)
        var seed = 1
        fun photo(iterationId: String, pos: Int, caption: String = "", top: Int = 0xFF5D4037.toInt(), bottom: Int = 0xFF3E2723.toInt()): List<PhotoEntity> =
            if (!withPhotos) emptyList() else listOf(
                PhotoEntity(RecipeRepository.newId(), iterationId, photos.writeJpeg(fakePhoto(seed++, top, bottom)), caption, pos, now),
            )

        // 1. Smash burgers: three iterations, improving
        val burger = RecipeEntity(
            id = "burger", title = "Smash burgers", description = "Crispy-edged double patties with house sauce.",
            category = "BBQ", tags = listOf("weekend", "crowd-pleaser"), servings = 4, prepMinutes = 20, cookMinutes = 15,
            source = "Inspired by a YouTube video", isFavorite = true, createdBy = "Maarten", createdAt = now, updatedAt = now,
        )
        val ingredientsV1 = listOf("500 g ground beef (20% fat)", "4 brioche buns", "4 slices cheddar", "1 onion, thinly sliced", "Salt & pepper")
        val stepsV1 = listOf("Divide the beef into 8 balls of 60 g.", "Heat the cast-iron pan until smoking.", "Smash each ball flat, season, cook 2 minutes.", "Flip, add cheese, stack two patties per bun.")
        val iterations = listOf(
            IterationEntity("b1", "burger", today.minusDays(40).toEpochDay(), "Maarten", ingredientsV1, stepsV1, 3f,
                "", "Use a hotter pan and fattier meat. Toast the buns.", "Patties stuck to the spatula. Taste was good but a bit dry.", now - 3000, now),
            IterationEntity("b2", "burger", today.minusDays(20).toEpochDay(), "Maarten & Pieter", ingredientsV1 + "Butter for the buns",
                stepsV1 + "Toast the buns in butter.", 4f, "Hotter pan, 20% fat beef, toasted buns.", "Make a proper burger sauce.", "Much better crust.", now - 2000, now),
            IterationEntity("b3", "burger", today.toEpochDay(), "Pieter",
                listOf("Patties:", "500 g ground beef (20% fat)", "Salt & pepper", "Sauce:", "3 tbsp mayonnaise", "1 tbsp ketchup", "1 tbsp chopped pickles", "1 tsp mustard", "Assembly:", "4 brioche buns", "4 slices cheddar", "1 onion, very thinly sliced", "Butter"),
                listOf(
                    "Mix the sauce ingredients and let them rest in the fridge.",
                    "Divide the beef into 8 loose balls of about 60 g. Keep them cold until the pan is ready.",
                    "Heat a cast-iron pan on the highest heat for 5 minutes until it smokes.",
                    "Put a ball in the pan, place onion slices on top and smash hard with a spatula for 10 seconds. Season with salt and pepper.",
                    "Cook 2 minutes until the edges are deeply brown and crispy, then scrape and flip.",
                    "Add cheese, stack two patties, and serve on the buttered toasted buns with plenty of sauce.",
                ),
                4.5f, "Added the burger sauce and smashed onions into the patties.", "Try potato buns. Maybe add bacon jam.",
                "Best version so far. The onions caramelised into the meat.", now - 1000, now),
        )
        repo.save(burger, iterations[0], photo("b1", 0, top = 0xFF795548.toInt()))
        repo.save(burger, iterations[1], photo("b2", 0))
        repo.save(burger, iterations[2], photo("b3", 0, "Double stack", 0xFF263238.toInt(), 0xFF37474F.toInt()) +
            photo("b3", 1, "Smashing the onions") + photo("b3", 2, "The sauce") + photo("b3", 3))
        repo.addRating("b2", "Mom", 4f, "")
        repo.addRating("b3", "Mom", 5f, "Better than the restaurant!")
        repo.addRating("b3", "Sarah", 4.5f, "Sauce is amazing")
        repo.addRating("b3", "Pieter", 4f, "Bun was a bit soft")

        // 2. Sourdough focaccia
        val focaccia = RecipeEntity("focaccia", "Rosemary focaccia", "Airy, olive-oily, crispy bottom.", "Baking", listOf("vegetarian"),
            8, 30, 25, "", false, "Pieter", now, now)
        repo.save(
            focaccia,
            IterationEntity("f1", "focaccia", today.minusDays(5).toEpochDay(), "Pieter",
                listOf("500 g bread flour", "400 g water", "10 g salt", "5 g instant yeast", "60 ml olive oil", "Fresh rosemary", "Flaky salt"),
                listOf("Mix flour, water, yeast and salt into a wet dough.", "Do 4 sets of stretch and folds, 30 minutes apart.", "Proof overnight in the fridge in an oiled tin.", "Dimple with oiled fingers, top with rosemary and flaky salt.", "Bake 25 minutes at 230 °C."),
                4f, "", "Use more oil in the tin for a crispier bottom.", "", now, now),
            photo("f1", 0, top = 0xFFD7CCC8.toInt(), bottom = 0xFFA1887F.toInt()),
        )
        repo.addRating("f1", "Sarah", 4.5f, "")

        // 3. Ramen, no photo
        val ramen = RecipeEntity("ramen", "Spicy miso ramen", "", "Soup", emptyList(), 2, 30, 60, "", false, "Maarten", now, now)
        repo.save(
            ramen,
            IterationEntity("r1", "ramen", today.minusDays(60).toEpochDay(), "Maarten",
                listOf("2 portions ramen noodles", "1 L chicken stock", "2 tbsp white miso", "1 tbsp chili oil", "2 soft-boiled eggs", "Spring onion"),
                listOf("Heat the stock and whisk in the miso.", "Cook the noodles.", "Assemble with eggs, spring onion and chili oil."),
                3.5f, "", "Make the eggs the day before and marinate them.", "", now, now),
            emptyList(),
        )
        // 4. Tiramisu
        val tiramisu = RecipeEntity("tiramisu", "Tiramisu", "The classic.", "Dessert", emptyList(), 6, 30, null, "Nonna's notebook", true, "Maarten", now, now)
        repo.save(
            tiramisu,
            IterationEntity("t1", "tiramisu", today.minusDays(10).toEpochDay(), "Maarten",
                listOf("250 g mascarpone", "3 eggs", "75 g sugar", "Ladyfingers", "Strong coffee", "Cocoa powder"),
                listOf("Whisk yolks with sugar, fold in mascarpone.", "Whip the whites and fold in.", "Dip ladyfingers in coffee and layer.", "Rest overnight, dust with cocoa."),
                5f, "", "", "", now, now),
            photo("t1", 0, top = 0xFF4E342E.toInt(), bottom = 0xFF8D6E63.toInt()),
        )
    }
}
