package com.serafino.data

import com.serafino.data.loyalty.LoyaltyResponseMapper
import com.serafino.data.loyalty.LoyaltyTierCatalog
import com.serafino.domain.entities.loyalty.BeanTransactionKind
import com.serafino.domain.entities.loyalty.CafeDelDia
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Espeja `LoyaltyResponseMapperTests` de iOS (parsing del programa de puntos). */
class LoyaltyResponseMapperTest {

    @Test fun mapsConfigTiers() {
        val config = """
        {"pointsName":"granos","amountPerPoint":1000,"tiers":[
          {"key":"base","label":"Base","min":0,"minLabel":"Desde 0 granos","color":"#5b4231","icon":"bean","tagline":"Empezá","cafeDelDiaPct":5},
          {"key":"intermedio","label":"Intermedio","min":300,"minLabel":"Desde 300 granos","color":"#a3692c","icon":"beans","tagline":"Seguí","cafeDelDiaPct":10},
          {"key":"top","label":"Top","min":700,"minLabel":"Desde 700 granos","color":"#c0833f","icon":"star","tagline":"Cima","cafeDelDiaPct":15}
        ]}
        """.trimIndent()
        val tiers = LoyaltyResponseMapper.tiers(config)!!
        assertEquals(3, tiers.size)
        val mid = tiers[1]
        assertEquals("intermedio", mid.id)
        assertEquals("Intermedio", mid.name)
        assertEquals(300, mid.threshold)
        assertEquals("Desde 300 granos", mid.minLabel)
        assertEquals("#a3692c", mid.accentHex)
        assertEquals(10, mid.cafeDelDiaPct)
        assertEquals(LoyaltyTierCatalog.symbol("beans"), mid.symbolName)
        assertEquals(LoyaltyTierCatalog.perks("intermedio"), mid.perks)
        assertEquals(15, tiers[2].cafeDelDiaPct)
    }

    @Test fun mapsPublicConfig() {
        val data = """
        {"pointsName":"granos","amountPerPoint":1000,
         "tiers":[{"key":"base","label":"Base","min":0,"cafeDelDiaPct":5}],
         "rewards":[{"id":"free-125","type":"free_product","cost":250,"unlockTier":"base","label":"Café Degustación 125 g","format":"125 g"}],
         "cafeDelDia":{"productId":"aurora","cost":60,"day":"tue","dayLabel":"Martes","pct":5,"maxDiscountARS":3000}}
        """.trimIndent()
        val config = LoyaltyResponseMapper.config(data)!!
        assertEquals("granos", config.pointsName)
        assertEquals(1000, config.amountPerPoint)
        assertEquals(5, config.tiers.first().cafeDelDiaPct)
        assertEquals("free-125", config.rewards.first().id)
        assertEquals(LoyaltyRewardType.FreeProduct, config.rewards.first().type)
        assertEquals(60, config.cafeDelDia?.cost)
        assertEquals("cafe-del-dia", config.cafeDelDia?.redeemId)
        assertNull(LoyaltyResponseMapper.config("{}"))
    }

    @Test fun mapsMeAccount() {
        val me = """
        {"pointsName":"granos","amountPerPoint":1000,"points":130,"lifetimePoints":130,
         "tier":"intermedio","tierLabel":"Intermedio","nextTier":"top","nextTierLabel":"Top",
         "pointsToNext":20,"toNextLabel":"Te faltan 20 granos para Top","progress":80}
        """.trimIndent()
        val acc = LoyaltyResponseMapper.account(me, LoyaltyTierCatalog.fallback, emptyList())
        assertEquals(130, acc.points)
        assertEquals(1000, acc.amountPerPoint)
        assertEquals("intermedio", acc.tierKey)
        assertEquals("top", acc.nextTierKey)
        assertEquals(20, acc.beansToNextTier)
        assertEquals("Te faltan 20 granos para Top", acc.toNextLabel)
        assertEquals(80, acc.progressPercent)
        assertEquals(0.8, acc.progress, 0.0001)
        assertEquals("Intermedio", acc.displayTier.name)
        assertEquals("top", acc.nextTier?.id)
    }

    @Test fun mapsLedger() {
        val ledger = """
        {"entries":[
          {"id":"e1","type":"earn","points":45,"orderId":"abc123XYZ789","orderLabel":"Aurora (250 g) + 1 más","createdAt":1718000000000},
          {"id":"e2","type":"bonus","points":5,"createdAt":1718000000000},
          {"id":"e3","type":"redeem","points":-8,"orderId":"ord9","createdAt":1718000000000}
        ]}
        """.trimIndent()
        val history = LoyaltyResponseMapper.history(ledger)
        assertEquals(3, history.size)
        assertEquals(listOf(BeanTransactionKind.Purchase, BeanTransactionKind.Bonus, BeanTransactionKind.Redeem), history.map { it.kind })
        assertEquals("abc123XYZ789", history[0].orderId)
        assertEquals("Aurora (250 g) + 1 más", history[0].orderLabel)
        assertNull(history[1].orderId)
        assertEquals(-8, history[2].amount)
        assertFalse(history[2].isCredit)
    }

    @Test fun tolerantDefaults() {
        assertNull(LoyaltyResponseMapper.tiers("{}"))
        assertTrue(LoyaltyResponseMapper.history("{}").isEmpty())
        val acc = LoyaltyResponseMapper.account("{}", LoyaltyTierCatalog.fallback, emptyList())
        assertEquals(0, acc.points)
        assertEquals(1000, acc.amountPerPoint)
        assertEquals("", acc.tierKey)
        assertEquals("granos", acc.pointsName)
        assertEquals("base", acc.currentTier.id)
        assertTrue(acc.rewards.isEmpty())
        assertNull(acc.cafeDelDia)
    }

    @Test fun mapsRewardsAndCafe() {
        val me = """
        {"points":450,"lifetimePoints":450,"tier":"intermedio","tierLabel":"Intermedio",
         "rewards":[
           {"id":"free-250","type":"free_product","cost":400,"costLabel":"400 granos","unlockTier":"intermedio","label":"Café Estándar 250g","format":"250 g","unlocked":true,"affordable":true,"canRedeem":true},
           {"id":"free-500","type":"free_product","cost":700,"unlockTier":"top","label":"Café Rendidor 500g","format":"500 g","unlocked":false,"affordable":false,"canRedeem":false}
         ],
         "cafeDelDia":{"productId":"aurora","cost":60,"costLabel":"60 granos","day":"tue","dayLabel":"Martes","pct":10,"maxDiscountARS":3000,"affordable":true}}
        """.trimIndent()
        val acc = LoyaltyResponseMapper.account(me, LoyaltyTierCatalog.fallback, emptyList())
        assertEquals(2, acc.rewards.size)
        assertEquals("free-250", acc.rewards[0].id)
        assertTrue(acc.rewards[0].canRedeem)
        assertFalse(acc.rewards[1].unlocked)
        assertEquals(10, acc.cafeDelDia?.pct)
        assertEquals("Martes", acc.cafeDelDia?.dayLabel)
        assertTrue(acc.cafeDelDia?.affordable == true)
    }

    @Test fun mapsVouchersAndRedeem() {
        val vouchers = """
        {"vouchers":[
          {"id":"v1","rewardId":"free-250","type":"free_product","cost":400,"label":"Café Estándar 250g","status":"active","format":"250 g","createdAt":1718000000000,"expiresAt":1730000000000}
        ]}
        """.trimIndent()
        val list = LoyaltyResponseMapper.vouchers(vouchers)
        assertEquals(1, list.size)
        assertEquals("v1", list[0].id)
        assertEquals(LoyaltyRewardType.FreeProduct, list[0].type)
        assertTrue(list[0].isActive)

        val redeem = """{"ok":true,"voucher":{"id":"v2","rewardId":"cafe-del-dia","type":"cafe_del_dia","cost":60,"label":"Café del Día","status":"active","pct":10},"balance":390}"""
        val outcome = LoyaltyResponseMapper.redeemOutcome(redeem)
        assertEquals(390, outcome?.balance)
        assertEquals(LoyaltyRewardType.CafeDelDia, outcome?.voucher?.type)
        assertEquals(10, outcome?.voucher?.pct)
    }

    @Test fun reversalIsNotPurchase() {
        val ledger = """{"entries":[{"id":"r1","type":"reversal","points":-45,"orderId":"abc123","orderLabel":"Aurora (250 g)","createdAt":1718000000000}]}"""
        val history = LoyaltyResponseMapper.history(ledger)
        assertEquals(1, history.size)
        assertEquals(BeanTransactionKind.Reversal, history[0].kind)
        assertEquals("reversal", history[0].type)
        assertEquals(-45, history[0].amount)
        assertFalse(history[0].isCredit)
        assertEquals("Reembolso", history[0].kind.label)
        // La reversión usa su propio ícono (token "undo"), distinto del de compra ("bag").
        assertEquals("undo", history[0].kind.symbolName)
    }

    @Test fun cafeConstantsAndRedeemId() {
        val me = """
        {"points":300,"lifetimePoints":300,"tier":"intermedio","tierLabel":"Intermedio",
         "rewards":[{"id":"merch-tote","type":"merch","cost":120,"unlockTier":"base","label":"Tote Serafino","unlocked":true,"affordable":true,"canRedeem":true}],
         "cafeDelDia":{"productId":"aurora","cost":60,"day":"tue","dayLabel":"Martes","pct":10,"maxDiscountARS":3000,"affordable":true}}
        """.trimIndent()
        val acc = LoyaltyResponseMapper.account(me, LoyaltyTierCatalog.fallback, emptyList())
        assertEquals(LoyaltyRewardType.Merch, acc.rewards.first().type)
        assertEquals(120, acc.rewards.first().cost)
        assertEquals(60, acc.cafeDelDia?.cost)
        assertEquals(3000, acc.cafeDelDia?.maxDiscountARS)
        assertEquals(CafeDelDia.rewardID, acc.cafeDelDia?.redeemId)
    }

    @Test fun floorsNegativeBalance() {
        val me = """{"points":-50,"lifetimePoints":-10,"tier":"base"}"""
        val acc = LoyaltyResponseMapper.account(me, LoyaltyTierCatalog.fallback, emptyList())
        assertEquals(0, acc.points)
        assertEquals(0, acc.lifetimePoints)
    }

    @Test fun mapsDirectRedeem() {
        val data = """{"ok":true,"orderId":"loyalty-order-7","voucher":{"id":"v1","type":"free_product","status":"redeemed"}}"""
        val outcome = LoyaltyResponseMapper.directRedeemOutcome(data)
        assertEquals("loyalty-order-7", outcome?.orderId)
        assertEquals("redeemed", outcome?.voucher?.status)
        assertNull(LoyaltyResponseMapper.directRedeemOutcome("{}"))
    }

    @Test fun fallbackLadderMatchesContract() {
        val ladder = LoyaltyTierCatalog.fallback
        assertEquals(listOf("base", "intermedio", "top"), ladder.map { it.id })
        assertEquals(listOf(0, 300, 700), ladder.map { it.threshold })
        assertEquals(listOf(5, 10, 15), ladder.map { it.cafeDelDiaPct })
        assertTrue(ladder.all { it.tagline.isNotEmpty() })
    }
}
