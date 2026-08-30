# Room database keep rules
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-keep interface androidx.room.* { *; }
-keep class androidx.room.Room { *; }
-dontwarn androidx.room.paging.**

# Keep Entity, DAO, Type Converters, and generated Room classes
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.converter.** { *; }
-keep class com.example.data.local.DatabaseHelper { *; }
-keep class com.example.data.local.DatabaseInitializer { *; }
-keep class com.example.data.local.KramaDatabase { *; }
-keep class com.example.data.local.KramaDatabase_Impl { *; }

# SQLCipher rules
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class net.sqlcipher.database.SQLiteDatabase { *; }
-keep class net.sqlcipher.database.SQLiteOpenHelper { *; }
-keep class net.sqlcipher.database.SQLiteCursor { *; }
-keep class net.sqlcipher.database.SQLiteStatement { *; }
-dontwarn net.sqlcipher.**

# Firebase SDK Keep Rules & Reflection Prevention
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Firebase Database & Firestore Serialization Rules
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
    @com.google.firebase.firestore.IgnoreExtraProperties <class>;
}

# Keep Native JNI Methods and Classes
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.example.util.NativeImageCompressor { *; }

# Matrix SDK, Startup Safety Monitor & Cryptography
-keep class com.example.util.StartupSafetyMonitor { *; }
-keep class com.example.util.MatrixRustSDK { *; }
-keep class com.example.data.repository.MatrixMessagingEngine { *; }
-keep class com.example.data.repository.SecurityRepository { *; }
-keep class com.example.data.repository.FirebasePresenceManager { *; }
-keep class com.example.data.security.** { *; }
-keep class com.example.data.remote.** { *; }
-keep class com.example.data.repository.** { *; }
-keep class com.example.domain.model.** { *; }

# Kotlin Serialization & Coroutines & Models
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * implements java.io.Serializable { *; }


