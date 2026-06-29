package com.serafino.domain

import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Vigencia del vale: espeja `decideVoucherUse` del backend (vence cuando `expiresAt <= now`).
 * El endpoint `/vouchers` NO filtra los vencidos, así que el cliente debe descartarlos.
 */
class LoyaltyVoucherTest {

    private val now = Date(1_000_000_000_000L)
    private fun voucher(status: String = "active", expiresAt: Date?) = LoyaltyVoucher(
        id = "v1", rewardId = "r1", type = LoyaltyRewardType.CafeDelDia, cost = 400, label = "Vale",
        status = status, format = "250 g", productId = "aurora", pct = 10, maxDiscountARS = null,
        createdAt = null, expiresAt = expiresAt,
    )

    @Test fun activeAndFutureIsUsable() {
        assertTrue(voucher(expiresAt = Date(now.time + 86_400_000L)).isUsable(now))
    }

    @Test fun activeAndExpiredIsNotUsable() {
        assertFalse(voucher(expiresAt = Date(now.time - 1L)).isUsable(now))
    }

    @Test fun expiringExactlyNowIsNotUsable() {
        // Backend: vence cuando expiresAt <= now; `after(now)` es false en el instante exacto.
        assertFalse(voucher(expiresAt = Date(now.time)).isUsable(now))
    }

    @Test fun noExpiryIsUsableWhileActive() {
        assertTrue(voucher(expiresAt = null).isUsable(now))
    }

    @Test fun redeemedIsNeverUsable() {
        assertFalse(voucher(status = "redeemed", expiresAt = Date(now.time + 86_400_000L)).isUsable(now))
    }
}
