# kotlinx.serialization: keep generated serializers for @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer { *; }

# Keep the app's own classes intact (routes, serializers, Room entities). Only library
# code gets shrunk, which is where almost all of the size win comes from.
-keep class com.recipetracker.** { *; }
