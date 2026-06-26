package com.serafino.data.persistence

import com.serafino.architecture.EventBus
import com.serafino.architecture.FavoriteToggledEvent
import com.serafino.domain.services.FavoritesStoring

/**
 * Persiste favoritos con Room y difunde los cambios por el [EventBus]. Mantiene un set en memoria
 * para que Room no se filtre a las vistas. Espeja `SwiftDataFavoritesStore` de iOS.
 */
class RoomFavoritesStore(
    private val dao: FavoritesDao,
    private val bus: EventBus,
) : FavoritesStoring {

    override var favoriteIDs: Set<String> = emptySet()
        private set

    init {
        favoriteIDs = dao.allIDs().toSet()
    }

    override fun isFavorite(id: String): Boolean = favoriteIDs.contains(id)

    override fun toggle(id: String) = setFavorite(id, !isFavorite(id))

    override fun setFavorite(id: String, value: Boolean) {
        if (value == isFavorite(id)) return
        if (value) {
            dao.insert(FavoriteRecipeEntity(recipeID = id, addedAt = System.currentTimeMillis()))
            favoriteIDs = favoriteIDs + id
        } else {
            dao.delete(id)
            favoriteIDs = favoriteIDs - id
        }
        bus.publish(FavoriteToggledEvent(recipeID = id, isFavorite = value))
    }
}
