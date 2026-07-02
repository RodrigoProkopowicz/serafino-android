package com.serafino.feature.store.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.EventBus
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.PurchaseCompletedEvent
import com.serafino.designsystem.Haptics
import com.serafino.domain.entities.store.OrderStatus
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.CheckoutService
import com.serafino.domain.services.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Al volver del Checkout Pro, consulta `GET /api/order/:id` con reintentos. Si el pago se
 * aprueba, vacía el carrito y publica [PurchaseCompletedEvent] para que el Perfil reconcilie
 * los granos de la compra. Espeja `OrderStatusInteractor` de iOS.
 */
class OrderStatusInteractor(
    orderID: String,
    private val checkout: CheckoutService,
    private val cart: CartStoring,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
    private val maxAttempts: Int = 8,
    private val retryDelayMillis: Long = 2000,
) : Interactor<OrderStatusInteractor.Data, OrderStatusInteractor.Input, OrderStatusInteractor.State> {

    data class Data(
        val orderId: String,
        val status: OrderStatus = OrderStatus.Pending,
        val totalText: String = "",
        val isPolling: Boolean = false,
    )

    sealed interface Input {
        data object OnAppear : Input
        data object Refresh : Input
    }

    enum class State { Loading, Loaded, Failed }

    override var data by mutableStateOf(Data(orderId = orderID))
        private set
    override var state by mutableStateOf(State.Loading)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear, is Input.Refresh -> if (!data.isPolling) poll()
        }
    }

    fun dispose() = scope.cancel()

    private fun poll() {
        data = data.copy(isPolling = true)
        scope.launch {
            for (attempt in 1..maxAttempts) {
                val result = runCatching { checkout.fetchOrder(data.orderId) }
                result.onSuccess { order ->
                    data = data.copy(status = order.status, totalText = Money.ars(order.total))
                    state = State.Loaded
                    if (!order.status.isPending) {
                        if (order.status == OrderStatus.Approved) {
                            cart.clear()
                            Haptics.success()
                            analytics.track(AnalyticsEvent.Purchase(data.orderId, order.total))
                            // Los granos los acredita el webhook: el Perfil escucha este
                            // evento y reconcilia con reintentos hasta ver el saldo nuevo.
                            bus.publish(PurchaseCompletedEvent(data.orderId))
                        }
                        data = data.copy(isPolling = false)
                        return@launch
                    }
                }.onFailure {
                    if (state != State.Loaded && attempt == maxAttempts) {
                        state = State.Failed
                        data = data.copy(isPolling = false)
                        return@launch
                    }
                }
                if (attempt < maxAttempts) delay(retryDelayMillis)
            }
            data = data.copy(isPolling = false)
        }
    }
}
