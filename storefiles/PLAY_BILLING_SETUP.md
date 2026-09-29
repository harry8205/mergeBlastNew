# Google Play purchases without app login

The app uses the account signed into Google Play. Firebase Authentication is not required. Existing Firebase analytics and crash reporting are unrelated to purchase ownership.

## Play Console setup

For package `com.mergeblast.game`, activate subscription `vip_monthly` with a monthly auto-renewing base plan and regional prices. The app selects a regular base plan, not a trial or promotional offer. Keep only the intended monthly base plan active for this product.

Activate the one-time products from `StoreProduct.catalog`: `remove_ads`, `starter_pack`, and the five `coins_*` packs. Coin packs are consumed; Remove Ads and Starter Pack are not consumed.

Upload a signed build to an internal testing track, add license testers, and install through Google Play using a tester account. Product configuration and real Play transactions cannot be verified from the local debug build.

## Behavior

- Query ownership on launch and whenever the activity resumes, including returning from Google's subscription management screen.
- Restore purchases manually from the store. Successful queries atomically refresh local ownership; failed queries keep the last saved state.
- Grant only PURCHASED items. Pending payments do not unlock benefits.
- Acknowledge non-consumables and subscriptions after saving access; consume coins after saving their grant. Failed confirmations retry on the next refresh.
- A subscription canceled but still returned by Play retains access through its paid period. When a successful query no longer returns it (expiration, hold, or revocation), remove VIP access.
- Show Active/Owned status and a Google Play subscription management link.
- Restore current VIP/permanent entitlements after reinstall using the purchasing Play account. Consumed coin balances, spent items, daily reward history, and game progress are local and do not sync across installs/devices.

## Verification before release

Test successful and pending payments, renewal, cancellation before expiration, expiration, payment hold, resubscription, refund/revocation, offline launch followed by reconnection, and reinstall/restore. Check that repeat queries do not repeat starter or coin grants. Repository regression tests are in `app/src/androidTest/java/com/mergeblast/ui/PlayOwnershipTest.kt` and require an Android device/emulator.

## Limits and future account linking

This is a client-only integration. Offline access uses the last successfully saved ownership state and cannot immediately detect expiry/refunds; local state is not tamper-proof. Before relying on stronger fraud protection or exact real-time subscription state, add secure backend purchase-token verification and Google Play real-time developer notifications. No service credentials belong in the APK.

Billing is isolated from account identity through BillingManager's grant/sync callbacks. A future Firebase login can link verified Play purchase tokens to a user on a backend without changing the Play product IDs. Define account-transfer and reward-sync rules during that migration; login alone does not implement these.

Reference: https://developer.android.com/google/play/billing/integrate
