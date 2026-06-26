package com.serafino.data.persistence

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Modelo Room que respalda los favoritos. Guarda solo el id de la receta; el contenido vive en el
 * catálogo estático. Espeja `FavoriteRecipe` (SwiftData) de iOS.
 */
@Entity(tableName = "favorite_recipe")
data class FavoriteRecipeEntity(
    @PrimaryKey val recipeID: String,
    val addedAt: Long,
)

/**
 * Modelo Room que respalda el carrito. La clave única es producto+formato (misma celda).
 * Espeja `CartLineModel` (SwiftData) de iOS.
 */
@Entity(tableName = "cart_line")
data class CartLineEntity(
    @PrimaryKey val id: String,
    val productID: String,
    val name: String,
    val image: String,
    val formatLabel: String,
    val formatWeight: String,
    val unitPrice: Int,
    val quantity: Int,
    val addedAt: Long,
)

@Dao
interface FavoritesDao {
    @Query("SELECT recipeID FROM favorite_recipe")
    fun allIDs(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: FavoriteRecipeEntity)

    @Query("DELETE FROM favorite_recipe WHERE recipeID = :id")
    fun delete(id: String)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_line ORDER BY addedAt ASC")
    fun all(): List<CartLineEntity>

    @Query("SELECT * FROM cart_line WHERE id = :id LIMIT 1")
    fun find(id: String): CartLineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: CartLineEntity)

    @Query("DELETE FROM cart_line WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM cart_line")
    fun clear()
}

@Database(
    entities = [FavoriteRecipeEntity::class, CartLineEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun cartDao(): CartDao
}

/**
 * Punto de entrada de la capa de persistencia: arma el `AppDatabase`. Habilita consultas en el
 * hilo principal para conservar la API SÍNCRONA de los stores (el análogo del `mainContext`
 * síncrono de SwiftData; el volumen de datos es trivial). Espeja `AppPersistence` de iOS.
 */
object AppPersistence {
    @Volatile
    private var instance: AppDatabase? = null

    fun database(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "serafino.db",
            )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
                .also { instance = it }
        }
}
