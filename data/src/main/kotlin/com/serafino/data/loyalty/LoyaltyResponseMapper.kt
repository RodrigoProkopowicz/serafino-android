package com.serafino.data.loyalty

import com.serafino.domain.entities.loyalty.BeanTransaction
import com.serafino.domain.entities.loyalty.BeanTransactionKind
import com.serafino.domain.entities.loyalty.CafeDelDia
import com.serafino.domain.entities.loyalty.DirectRedeemOutcome
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyConfig
import com.serafino.domain.entities.loyalty.LoyaltyReward
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.loyalty.LoyaltyTier
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.loyalty.RedeemOutcome
import org.json.JSONObject
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * Parsea las respuestas del programa de puntos al dominio, mostrando TAL CUAL lo que trae el
 * backend. PURO y testeable (sin red). Espeja `LoyaltyResponseMapper` de iOS.
 */
object LoyaltyResponseMapper {

    // MARK: - /config → escalera

    fun tiers(configBody: String): List<LoyaltyTier>? {
        val arr = json(configBody).optJSONArray("tiers") ?: return null
        if (arr.length() == 0) return null
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { tier(it) }
    }

    private fun tier(dto: JSONObject): LoyaltyTier {
        val key = dto.optStringOrNull("key") ?: ""
        val minV = max(0, dto.optIntOrNull("min") ?: 0)
        return LoyaltyTier(
            id = key,
            name = dto.optStringOrNull("label") ?: key,
            tagline = dto.optStringOrNull("tagline") ?: "",
            threshold = minV,
            minLabel = dto.optStringOrNull("minLabel") ?: "Desde $minV granos",
            perks = LoyaltyTierCatalog.perks(key),
            symbolName = LoyaltyTierCatalog.symbol(dto.optStringOrNull("icon")),
            accentHex = dto.optStringOrNull("color") ?: "C0833F",
            cafeDelDiaPct = dto.optIntOrNull("cafeDelDiaPct"),
        )
    }

    // MARK: - /config → config pública (pre-login)

    fun config(configBody: String): LoyaltyConfig? {
        val obj = runCatching { JSONObject(configBody) }.getOrNull() ?: return null
        val tiersArr = obj.optJSONArray("tiers")
        val tiers = if (tiersArr != null) (0 until tiersArr.length()).mapNotNull { tiersArr.optJSONObject(it) }.map { tier(it) } else emptyList()
        if (tiers.isEmpty()) return null
        val perPoint = obj.optIntOrNull("amountPerPoint") ?: 0
        val rewardsArr = obj.optJSONArray("rewards")
        val rewards = if (rewardsArr != null) (0 until rewardsArr.length()).mapNotNull { rewardsArr.optJSONObject(it) }.map { reward(it) } else emptyList()
        return LoyaltyConfig(
            pointsName = obj.optStringOrNull("pointsName")?.takeIf { it.isNotEmpty() } ?: "granos",
            amountPerPoint = if (perPoint > 0) perPoint else 1000,
            tiers = tiers,
            rewards = rewards,
            cafeDelDia = obj.optJSONObject("cafeDelDia")?.let { cafeDelDia(it) },
        )
    }

    // MARK: - /me (+ escalera + historial) → cuenta

    fun account(meBody: String, tiers: List<LoyaltyTier>, history: List<BeanTransaction>): LoyaltyAccount {
        val me = json(meBody)
        val perPoint = me.optIntOrNull("amountPerPoint") ?: 0
        val rewardsArr = me.optJSONArray("rewards")
        val rewards = if (rewardsArr != null) (0 until rewardsArr.length()).mapNotNull { rewardsArr.optJSONObject(it) }.map { reward(it) } else emptyList()
        return LoyaltyAccount(
            pointsName = me.optStringOrNull("pointsName")?.takeIf { it.isNotEmpty() } ?: "granos",
            points = max(0, me.optIntOrNull("points") ?: 0),
            lifetimePoints = max(0, me.optIntOrNull("lifetimePoints") ?: 0),
            amountPerPoint = if (perPoint > 0) perPoint else 1000,
            tierKey = me.optStringOrNull("tier") ?: "",
            tierLabel = me.optStringOrNull("tierLabel") ?: "",
            nextTierKey = me.optStringOrNull("nextTier"),
            nextTierLabel = me.optStringOrNull("nextTierLabel"),
            pointsToNext = max(0, me.optIntOrNull("pointsToNext") ?: 0),
            toNextLabel = me.optStringOrNull("toNextLabel"),
            progressPercent = min(100, max(0, me.optIntOrNull("progress") ?: 0)),
            tiers = tiers,
            rewards = rewards,
            cafeDelDia = me.optJSONObject("cafeDelDia")?.let { cafeDelDia(it) },
            history = history,
        )
    }

    // MARK: - /ledger → movimientos

    fun history(ledgerBody: String): List<BeanTransaction> {
        val arr = json(ledgerBody).optJSONArray("entries") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { transaction(it) }
    }

    // MARK: - /vouchers → vales

    fun vouchers(body: String): List<LoyaltyVoucher> {
        val arr = json(body).optJSONArray("vouchers") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { voucher(it) }
    }

    // MARK: - /redeem → resultado

    fun redeemOutcome(body: String): RedeemOutcome? {
        val obj = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val v = obj.optJSONObject("voucher") ?: return null
        return RedeemOutcome(voucher = voucher(v), balance = obj.optIntOrNull("balance"))
    }

    // MARK: - /redeem/ship → canje directo

    fun directRedeemOutcome(body: String): DirectRedeemOutcome? {
        val obj = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val orderId = obj.optStringOrNull("orderId")
        val v = obj.optJSONObject("voucher")
        if (orderId == null && v == null) return null
        return DirectRedeemOutcome(orderId = orderId, voucher = v?.let { voucher(it) })
    }

    // MARK: - Builders

    private fun reward(dto: JSONObject): LoyaltyReward = LoyaltyReward(
        id = dto.optStringOrNull("id") ?: "",
        type = LoyaltyRewardType.fromRaw(dto.optStringOrNull("type") ?: "free_product"),
        cost = dto.optIntOrNull("cost") ?: 0,
        costLabel = dto.optStringOrNull("costLabel") ?: "${dto.optIntOrNull("cost") ?: 0} granos",
        unlockTierKey = dto.optStringOrNull("unlockTier") ?: "base",
        label = dto.optStringOrNull("label") ?: "",
        format = dto.optStringOrNull("format"),
        unlocked = dto.optBoolean("unlocked", false),
        affordable = dto.optBoolean("affordable", false),
        canRedeem = dto.optBoolean("canRedeem", false),
    )

    private fun cafeDelDia(dto: JSONObject): CafeDelDia = CafeDelDia(
        productId = dto.optStringOrNull("productId"),
        cost = dto.optIntOrNull("cost") ?: 0,
        costLabel = dto.optStringOrNull("costLabel") ?: "${dto.optIntOrNull("cost") ?: 0} granos",
        day = dto.optStringOrNull("day") ?: "",
        dayLabel = dto.optStringOrNull("dayLabel") ?: "",
        pct = dto.optIntOrNull("pct") ?: 0,
        maxDiscountARS = dto.optIntOrNull("maxDiscountARS"),
        affordable = dto.optBoolean("affordable", false),
    )

    private fun voucher(dto: JSONObject): LoyaltyVoucher = LoyaltyVoucher(
        id = dto.optStringOrNull("id") ?: "",
        rewardId = dto.optStringOrNull("rewardId"),
        type = LoyaltyRewardType.fromRaw(dto.optStringOrNull("type") ?: ""),
        cost = dto.optIntOrNull("cost") ?: 0,
        label = dto.optStringOrNull("label") ?: "",
        status = dto.optStringOrNull("status") ?: "active",
        format = dto.optStringOrNull("format"),
        productId = dto.optStringOrNull("productId"),
        pct = dto.optIntOrNull("pct"),
        maxDiscountARS = dto.optIntOrNull("maxDiscountARS"),
        createdAt = dto.optDoubleOrNull("createdAt")?.let { Date(it.toLong()) },
        expiresAt = dto.optDoubleOrNull("expiresAt")?.let { Date(it.toLong()) },
    )

    private fun transaction(entry: JSONObject): BeanTransaction {
        val type = entry.optStringOrNull("type") ?: "earn"
        val orderId = entry.optStringOrNull("orderId")
        val kind = when {
            type == "reversal" -> BeanTransactionKind.Reversal
            type == "redeem" -> BeanTransactionKind.Redeem
            orderId != null -> BeanTransactionKind.Purchase
            else -> BeanTransactionKind.Bonus
        }
        val date = entry.optDoubleOrNull("createdAt")?.let { Date(it.toLong()) } ?: Date(0)
        return BeanTransaction(
            id = entry.optStringOrNull("id") ?: orderId ?: date.time.toString(),
            type = type,
            orderId = orderId,
            orderLabel = entry.optStringOrNull("orderLabel"),
            date = date,
            amount = entry.optIntOrNull("points") ?: 0,
            kind = kind,
        )
    }

    // MARK: - Helpers org.json (tolerantes)

    private fun json(body: String): JSONObject = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) optString(key) else null
    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val v = opt(key)) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }
    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return when (val v = opt(key)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }
}
