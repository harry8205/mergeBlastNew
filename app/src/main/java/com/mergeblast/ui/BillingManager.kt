package com.mergeblast.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import com.android.billingclient.api.*
import com.mergeblast.data.models.StoreProduct
import com.mergeblast.data.models.PurchaseGrant

data class StoreOffer(val product: StoreProduct, val price: String, val details: ProductDetails, val offerToken: String?)

class BillingManager(
    private val activity: Activity,
    private val grant: (String, String, Long, Int, () -> Unit) -> Unit,
    private val sync: (List<String>, List<PurchaseGrant>, () -> Unit) -> Unit
) : PurchasesUpdatedListener {
    var offers by mutableStateOf<List<StoreOffer>>(emptyList()); private set
    var message by mutableStateOf<String?>(null)
    var restoring by mutableStateOf(false); private set
    private var connecting = false
    private var closed = false
    private var notifyRestore = false
    private val processing = mutableSetOf<String>()
    private val consumables = setOf("coins_1000", "coins_2500", "coins_5000", "coins_10000", "coins_25000")
    private val client = BillingClient.newBuilder(activity).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection().build()

    fun connect() {
        if (closed || connecting) return
        if (client.isReady) { query(); restore(); return }
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() { connecting = false }
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (closed) return
                if (result.responseCode == BillingClient.BillingResponseCode.OK) { query(); restore() }
                else {
                    message = "Google Play is unavailable. Check your connection and try Restore purchases again."
                    notifyRestore = false
                }
            }
        })
    }

    fun close() { closed = true; client.endConnection() }

    fun manageSubscription() {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
            .buildUpon().appendQueryParameter("sku", "vip_monthly")
            .appendQueryParameter("package", activity.packageName).build()
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (_: android.content.ActivityNotFoundException) { message = "Open Google Play > Payments & subscriptions > Subscriptions to manage VIP." }
    }

    fun restorePurchases() {
        notifyRestore = true
        if (client.isReady) restore() else connect()
    }

    private fun query() {
        val inapp = StoreProduct.catalog.filter { it.id != "vip_monthly" }.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it.id).setProductType(BillingClient.ProductType.INAPP).build()
        }
        val subs = listOf(QueryProductDetailsParams.Product.newBuilder().setProductId("vip_monthly").setProductType(BillingClient.ProductType.SUBS).build())
        fun load(products: List<QueryProductDetailsParams.Product>) {
            client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, response ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    offers = (offers + response.productDetailsList.mapNotNull { d ->
                        val model = StoreProduct.catalog.firstOrNull { it.id == d.productId } ?: return@mapNotNull null
                        val one = d.oneTimePurchaseOfferDetailsList?.firstOrNull()
                        // Use the regular base plan so the displayed monthly price is unambiguous.
                        val sub = d.subscriptionOfferDetails?.firstOrNull { it.offerId == null }
                        if (d.productType == BillingClient.ProductType.SUBS && sub == null) return@mapNotNull null
                        val price = one?.formattedPrice ?: sub?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "Unavailable"
                        StoreOffer(model, price, d, one?.offerToken ?: sub?.offerToken)
                    }).associateBy { it.product.id }.values.toList()
                }
            }
        }
        load(inapp); load(subs)
    }

    fun buy(id: String) {
        if (!client.isReady) { message = "Connecting to Google Play. Please try again."; connect(); return }
        val offer = offers.firstOrNull { it.product.id == id } ?: run { message = "Product unavailable"; return }
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(offer.details)
        offer.offerToken?.let(builder::setOfferToken)
        val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(builder.build())).build())
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) restorePurchases()
        else if (result.responseCode != BillingClient.BillingResponseCode.OK) message = "Unable to start purchase. Please try again."
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.orEmpty().forEach { process(it) }
        else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) restorePurchases()
        else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) message = "Purchase could not be completed. Please try again."
    }

    private fun restore() {
        if (closed || restoring) return
        restoring = true
        var remaining = 2
        var failed = false
        var ownedCount = 0
        fun finished() {
            remaining--
            if (remaining != 0) return
            restoring = false
            if (notifyRestore) {
                message = when {
                    failed -> "Could not check all purchases. Check your connection and try again."
                    ownedCount > 0 -> "Purchases restored from your Google Play account."
                    else -> "No active purchases found. Use the Google Play account you purchased with."
                }
            }
            notifyRestore = false
        }
        listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS).forEach { type ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                if (closed) return@queryPurchasesAsync
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val ownedProducts = if (type == BillingClient.ProductType.SUBS) listOf("vip_monthly") else listOf("remove_ads", "starter_pack")
                    val owned = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    val grants = owned.flatMap { p -> p.products.filter { it in ownedProducts }.map { id ->
                        PurchaseGrant(p.purchaseToken + id, id, p.purchaseTime)
                    } }
                    ownedCount += grants.size
                    sync(ownedProducts, grants) {
                        owned.filter { p -> p.products.any { it in ownedProducts } }.forEach { complete(it, false) }
                        owned.filter { p -> p.products.any { it in consumables } }.forEach { process(it, false) }
                        finished()
                    }
                } else { failed = true; finished() }
            }
        }
    }

    private fun process(p: Purchase, announce: Boolean = true) {
        if (p.purchaseState == Purchase.PurchaseState.PENDING && announce) message = "Payment pending. Your items will unlock after Google Play confirms payment."
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val ids = p.products.filter { id -> StoreProduct.catalog.any { it.id == id } }
        if (ids.isEmpty() || !processing.add(p.purchaseToken)) return
        var remaining = ids.size
        ids.forEach { id -> grant(p.purchaseToken + id, id, p.purchaseTime, p.quantity) {
            remaining--
            if (remaining == 0) {
                processing.remove(p.purchaseToken)
                complete(p, announce)
            }
        } }
    }

    private fun complete(p: Purchase, announce: Boolean) {
        if (closed) return
        fun completed(result: BillingResult) {
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                message = "Purchase saved. Google Play confirmation will retry when you reopen the app or restore purchases."
            } else if (announce) message = "Purchase complete"
        }
        if (p.products.all { it in consumables }) client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { result, _ -> completed(result) }
        else if (!p.isAcknowledged) client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build(), ::completed)
        else if (announce) message = "Purchase complete"
    }
}
