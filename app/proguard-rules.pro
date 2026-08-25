# Room generates code reflectively referenced at runtime.
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * { @androidx.room.* <methods>; }

# kotlinx.serialization generates a companion .serializer() per @Serializable class and looks it up
# reflectively. Without this the backup file writes fine in debug and blows up in release, which is
# the worst possible place to find out.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.idoelbak.tracker.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.idoelbak.tracker.data.**$$serializer { *; }
