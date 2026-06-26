package com.serafino.data.loyalty

import com.serafino.domain.entities.loyalty.BeanTransaction
import com.serafino.domain.entities.loyalty.BeanTransactionKind
import com.serafino.domain.entities.loyalty.CafeDelDia
import com.serafino.domain.entities.loyalty.DirectRedeemOutcome
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyConfig
import com.serafino.domain.entities.loyalty.LoyaltyReward
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.loyalty.RedeemOutcome
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.LoyaltyProviding
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Date

/**
 * Implementación de muestra de `LoyaltyProviding`: devuelve una cuenta de puntos creíble sin
 * backend (dev/preview). Espeja `SampleLoyaltyProvider` de iOS.
 */
class SampleLoyaltyProvider : LoyaltyProviding {

    override suspend fun loadPublicConfig(): LoyaltyConfig {
        delay(250)
        return LoyaltyConfig(
            pointsName = "granos",
            amountPerPoint = 1000,
            tiers = LoyaltyTierCatalog.fallback,
            rewards = sampleRewards,
            cafeDelDia = sampleCafeDelDia,
        )
    }

    override suspend fun loadAccount(): LoyaltyAccount {
        delay(450)
        // 450 granos acumulados → nivel "Intermedio" (≥300), 37% rumbo a "Top" (700).
        return LoyaltyAccount.make(
            points = 450,
            lifetimePoints = 450,
            amountPerPoint = 1000,
            tiers = LoyaltyTierCatalog.fallback,
            rewards = sampleRewards,
            cafeDelDia = sampleCafeDelDia,
            history = listOf(
                BeanTransaction("9bK2pQ7mD4xR1aT8nW0e", "earn", "9bK2pQ7mD4xR1aT8nW0e", "Aurora (250 g) + 1 más", date(2026, 6, 18), 45, BeanTransactionKind.Purchase),
                BeanTransaction("5hY8cF3jL6vB2nM9pQ1r", "earn", "5hY8cF3jL6vB2nM9pQ1r", "Combo Degustación", date(2026, 6, 9), 30, BeanTransactionKind.Purchase),
                BeanTransaction("2tR7wD9kX4mN6bV8cZ3s", "earn", "2tR7wD9kX4mN6bV8cZ3s", "Dolce (250 g)", date(2026, 5, 27), 25, BeanTransactionKind.Purchase),
                BeanTransaction("8pL3nQ5xJ7dF1gH9kR2w", "earn", "8pL3nQ5xJ7dF1gH9kR2w", "Nocturno (250 g)", date(2026, 5, 12), 30, BeanTransactionKind.Purchase),
            ),
        )
    }

    override suspend fun redeem(rewardId: String, idempotencyKey: String?): RedeemOutcome {
        delay(400)
        val label = if (rewardId == CafeDelDia.rewardID) "Café del Día" else "Premio canjeado"
        val voucher = LoyaltyVoucher(
            id = "dev-$rewardId", rewardId = rewardId, type = LoyaltyRewardType.FreeProduct, cost = 0,
            label = label, status = "active", format = null, productId = null, pct = null,
            maxDiscountARS = null, createdAt = null, expiresAt = null,
        )
        return RedeemOutcome(voucher = voucher, balance = null)
    }

    override suspend fun redeemDirect(
        voucherId: String,
        productId: String?,
        grind: String?,
        contact: CheckoutForm,
    ): DirectRedeemOutcome {
        delay(400)
        return DirectRedeemOutcome(orderId = "dev-order-$voucherId", voucher = null)
    }

    override suspend fun vouchers(): List<LoyaltyVoucher> {
        delay(200)
        val issued = date(2026, 6, 1)
        val expires = Date(issued.time + 60L * 24 * 3600 * 1000)
        return listOf(
            LoyaltyVoucher(
                id = "v-free-250", rewardId = "free-250", type = LoyaltyRewardType.FreeProduct, cost = 400,
                label = "Café Estándar 250 g", status = "active", format = "250 g",
                productId = null, pct = null, maxDiscountARS = null, createdAt = issued, expiresAt = expires,
            ),
            LoyaltyVoucher(
                id = "v-tote", rewardId = "merch-tote", type = LoyaltyRewardType.Merch, cost = 120,
                label = "Tote Serafino", status = "active", format = null,
                productId = null, pct = null, maxDiscountARS = null, createdAt = issued, expiresAt = expires,
            ),
        )
    }

    private companion object {
        val sampleRewards: List<LoyaltyReward> = listOf(
            reward("free-125", LoyaltyRewardType.FreeProduct, 250, "base", "Café Degustación 125g", "125 g", unlocked = true, affordable = true),
            reward("free-250", LoyaltyRewardType.FreeProduct, 400, "intermedio", "Café Estándar 250g", "250 g", unlocked = true, affordable = true),
            reward("free-500", LoyaltyRewardType.FreeProduct, 700, "top", "Café Rendidor 500g", "500 g", unlocked = false, affordable = false),
            reward("merch-tote", LoyaltyRewardType.Merch, 120, "base", "Tote Serafino", null, unlocked = true, affordable = true),
            reward("merch-taza", LoyaltyRewardType.Merch, 180, "intermedio", "Taza Serafino", null, unlocked = true, affordable = true),
        )

        val sampleCafeDelDia = CafeDelDia(
            productId = "aurora", cost = 60, costLabel = "60 granos",
            day = "tue", dayLabel = "Martes", pct = 10, maxDiscountARS = 3000, affordable = true,
        )

        fun reward(
            id: String,
            type: LoyaltyRewardType,
            cost: Int,
            unlock: String,
            label: String,
            format: String?,
            unlocked: Boolean,
            affordable: Boolean,
        ): LoyaltyReward = LoyaltyReward(
            id = id, type = type, cost = cost, costLabel = "$cost granos",
            unlockTierKey = unlock, label = label, format = format,
            unlocked = unlocked, affordable = affordable, canRedeem = unlocked && affordable,
        )

        fun date(year: Int, month: Int, day: Int): Date {
            val cal = Calendar.getInstance()
            cal.clear()
            cal.set(year, month - 1, day, 12, 0, 0)
            return cal.time
        }
    }
}
