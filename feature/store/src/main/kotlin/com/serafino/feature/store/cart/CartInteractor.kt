package com.serafino.feature.store.cart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.CartChangedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.Interactor
import com.serafino.architecture.events
import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.store.CartLine
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Refleja el carrito local: líneas con cantidad y subtotal. Las mutaciones van al CartStoring y
 * la reconstrucción se dispara por `CartChangedEvent`. Espeja `CartInteractor` de iOS.
 */
class CartInteractor(
    private val cart: CartStoring,
    private val bus: EventBus,
) : Interactor<CartInteractor.Data, CartInteractor.Input, CartInteractor.State> {

    data class LineModel(
        val id: String,
        val name: String,
        val imageURL: String?,
        val formatText: String,
        val unitPriceText: String,
        val lineTotalText: String,
        val quantity: Int,
    )

    data class Data(
        val lines: List<LineModel> = emptyList(),
        val subtotalText: String = "",
        val count: Int = 0,
    )

    sealed interface Input {
        data object OnAppear : Input
        data class Increment(val id: String) : Input
        data class Decrement(val id: String) : Input
        data class Remove(val id: String) : Input
    }

    enum class State { Loaded, Empty }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf(State.Empty)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        bindEvents()
        rebuild()
    }

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> rebuild()
            is Input.Increment ->
                cart.setQuantity(input.id, min(CartLine.MAX_ITEM_QUANTITY, quantity(input.id) + 1))
            is Input.Decrement -> cart.setQuantity(input.id, quantity(input.id) - 1)
            is Input.Remove -> cart.remove(input.id)
        }
    }

    fun dispose() = scope.cancel()

    private fun quantity(id: String): Int = cart.lines.firstOrNull { it.id == id }?.quantity ?: 0

    private fun rebuild() {
        val lines = cart.lines.map { line ->
            LineModel(
                id = line.id,
                name = line.name,
                imageURL = BackendConfig.imageURL(line.image),
                formatText = formatText(line),
                unitPriceText = Money.ars(line.unitPrice),
                lineTotalText = Money.ars(line.lineTotal),
                quantity = line.quantity,
            )
        }
        data = Data(lines = lines, subtotalText = Money.ars(cart.subtotal), count = cart.count)
        state = if (lines.isEmpty()) State.Empty else State.Loaded
    }

    private fun formatText(line: CartLine): String =
        listOf(line.formatLabel, line.formatWeight).filter { it.isNotEmpty() }.joinToString(" · ")

    private fun bindEvents() {
        scope.launch { bus.events<CartChangedEvent>().collect { rebuild() } }
    }
}
