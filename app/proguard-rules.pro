# Room generates code reflectively referenced at runtime.
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * { @androidx.room.* <methods>; }
