package com.serafino.data.persistence

import com.serafino.architecture.CartChangedEvent
import com.serafino.architecture.EventBus
import com.serafino.domain.entities.store.CartLine
import com.serafino.domain.services.CartStoring

/**
 * Persiste el carrito con Room y publica `CartChangedEvent`. Expone una lista en memoria para que
 * Room no se filtre a las vistas. Espeja `SwiftDataCartStore` de iOS.
 */
class RoomCartStore(
    private val dao: CartDao,
    private val bus: EventBus,
) : CartStoring {

    override var lines: List<CartLine> = emptyList()
        private set

    init {
        reload()
    }

    override val count: Int get() = lines.sumOf { it.quantity }
    override val subtotal: Int get() = lines.sumOf { it.lineTotal }

    override fun add(line: CartLine) {
        val existing = dao.find(line.id)
        if (existing != null) {
            dao.upsert(existing.copy(quantity = existing.quantity + line.quantity))
        } else {
            dao.upsert(line.toEntity())
        }
        commit()
    }

    override fun setQuantity(id: String, quantity: Int) {
        val model = dao.find(id) ?: return
        if (quantity <= 0) dao.delete(id) else dao.upsert(model.copy(quantity = quantity))
        commit()
    }

    override fun remove(id: String) {
        dao.delete(id)
        commit()
    }

    override fun clear() {
        dao.clear()
        commit()
    }

    private fun commit() {
        reload()
        bus.publish(CartChangedEvent(count = count))
    }

    private fun reload() {
        lines = dao.all().map { it.toCartLine() }
    }

    private fun CartLine.toEntity(): CartLineEntity = CartLineEntity(
        id = id,
        productID = productID,
        name = name,
        image = image,
        formatLabel = formatLabel,
        formatWeight = formatWeight,
        unitPrice = unitPrice,
        quantity = quantity,
        discounted = discounted,
        addedAt = System.currentTimeMillis(),
    )

    private fun CartLineEntity.toCartLine(): CartLine = CartLine(
        productID = productID,
        name = name,
        image = image,
        formatLabel = formatLabel,
        formatWeight = formatWeight,
        unitPrice = unitPrice,
        quantity = quantity,
        discounted = discounted,
    )
}
