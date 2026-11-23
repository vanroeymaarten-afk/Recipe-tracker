# Recipe Tracker

An Android app for logging the food you cook, so every attempt becomes a reference for the next one.

- **Recipes and iterations.** A recipe is the dish. Each time you cook it, you add an iteration that records exactly how you made it that time: ingredients, steps, photos, who cooked, your own rating, notes, *what you changed* and *what to change next time*.
- **Cook it again** copies the ingredients and method of an iteration into a new one, and shows the "next time" note you wrote last time as a reminder.
- **Ratings.** You give your own star rating (half stars allowed). **Let others rate** opens a big, simple "pass the phone" screen where guests enter their name, stars and an optional comment. The recipe page shows everyone's ratings and a chart of how the score changed over the iterations.
- **Photos** from the camera or gallery. They're resized and stored privately inside the app.
- **PDF export.** On the home screen, tap the PDF icon or long-press recipes to select them, then tap *Create PDF*. You get a styled cookbook: optional cover and contents page, the main photo, ingredients next to a numbered method, notes and next-time tips, ratings, a photo gallery and the iteration history. You can open, save or share it (WhatsApp, e-mail, Drive…).
- **Backup and sharing.** *Settings → Back up* writes all recipes and photos to one `.zip` file. *Import* merges such a file: new recipes are added, the newest edit of each recipe wins, and nothing is ever deleted. To send just a few recipes to someone else who uses the app, select them on the home screen and tap the share icon.
- Search by dish, ingredient or tag. Filter by favourites or category, and sort by recently cooked, best rated, most cooked or A–Z.

All data stays on the phone. There's no account and no server.

## Install on your phone

1. Copy `RecipeTracker.apk` to the phone (or download it there).
2. Open it. Android asks once to allow installing apps from that source (Files / Chrome). Allow it.
3. To update later, install a newer APK the same way. Your recipes are kept.

Both debug and release builds are signed with `app/debug.keystore`, which is committed on purpose: any build from any computer can update the installed app in place. Without it you'd have to uninstall first, and uninstalling deletes the recipes. Its passwords are Android's standard public debug values, so it isn't a secret.

## Build it yourself

Open the folder in Android Studio and press Run with the phone connected over USB (USB debugging on). Or from a terminal:

```
./gradlew assembleRelease       # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest     # tests; also renders sample PDF pages and screens to app/build/screenshots
```

Stack: Kotlin, Jetpack Compose (Material 3), Room, Coil, kotlinx.serialization. The PDF is drawn with Android's `PdfDocument` (see `app/src/main/java/com/recipetracker/pdf`).

## Toward sharing with each other

The data model is ready for a shared database later. Every recipe, iteration, photo and rating has a globally unique id, timestamps and an author, so records from two phones never collide, and the zip import already merges them this way. A later version could sync the same records through a small online backend instead of zip files.

When changing the database schema, bump the version in `AppDatabase` and add a migration. The exported schemas are in `app/schemas`. Never use a destructive migration: it would wipe everyone's recipes.
