package eu.tortitas.stash.routes

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Invoice
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent.PaymentMethodOptions
import com.stripe.model.Source.SourceOrder.Item
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCreateParams
import eu.tortitas.stash.plugins.provideLinkService
import eu.tortitas.stash.plugins.provideUserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod
import java.util.*

const val STRIPE_TIER_1_PRICE_ID = "price_1POOEvJ71FWx9p48qqweV043"
const val STRIPE_TIER_2_PRICE_ID = "price_1POOFpJ71FWx9p48pbEwJW6q"
const val STRIPE_TIER_3_PRICE_ID = "price_1POOGAJ71FWx9p48g56Ivhs4"

fun Route.stripeRoute(application: Application) {
    val userService = application.provideUserService()
    //val endpointSecret = "whsec_cV5iYGLTpodeG74faqHZtbVN0dvUKiBG"
    val endpointSecret = "whsec_0484b70c26166c603e2a3cdb66aed8dbad5e7c43c6957bf375aac0d7b278b726"

    route("/stripe") {
        authenticate {
            get("/intent") {
                Stripe.apiKey = "sk_test_51POOCGJ71FWx9p48twyAnxoXqbakJOG9sE0bLRWDUAHD73F1dtrxaOLmNSeZryOfPJpVcXuARO4Qf3Ab59MQ3E4O00lcmyiCZV";

                val user =
                    userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())
                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@get
                }

                val stripe_price_id = when (call.request.queryParameters["tier"]) {
                    "1" -> STRIPE_TIER_1_PRICE_ID
                    "2" -> STRIPE_TIER_2_PRICE_ID
                    "3" -> STRIPE_TIER_3_PRICE_ID
                    else -> null
                }
                if (stripe_price_id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid tier")
                    return@get
                }

                var stripeCustomerId = user.stripeCustomerId

                if (stripeCustomerId == null) {
                    val paramsCustomer =
                        CustomerCreateParams.builder()
                            .setName(user.username)
                            .setEmail(user.email)
                            .build()
                    val customer: Customer = Customer.create(paramsCustomer)

                    userService.update(user.id, user.copy(stripeCustomerId = customer.id))
                    stripeCustomerId = customer.id
                }

                val paymentSettings = SubscriptionCreateParams.PaymentSettings.builder()
                    .setSaveDefaultPaymentMethod(SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                    .build()

                val subCreateParams = SubscriptionCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .addItem(
                        SubscriptionCreateParams.Item.builder()
                            .setPrice(stripe_price_id)
                            .setQuantity(1)
                            .build()
                    )
                    .setPaymentSettings(paymentSettings)
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .addAllExpand(Arrays.asList("latest_invoice.payment_intent"))
                    .build()


                val subscription = Subscription.create(subCreateParams)
                val clientSecret = subscription.latestInvoiceObject.paymentIntentObject.clientSecret

                call.respond(HttpStatusCode.OK, hashMapOf("clientSecret" to clientSecret, "stripeSubscriptionId" to subscription.id))
            }
        }

        post("/webhook") {
            Stripe.apiKey = "sk_test_51POOCGJ71FWx9p48twyAnxoXqbakJOG9sE0bLRWDUAHD73F1dtrxaOLmNSeZryOfPJpVcXuARO4Qf3Ab59MQ3E4O00lcmyiCZV";

            val payload = call.receiveText()
            val sigHeader = call.request.headers["Stripe-Signature"]
            val event = Webhook.constructEvent(payload, sigHeader, endpointSecret)

            val dataObjectDeserializer = event.dataObjectDeserializer
            val stripeObject = dataObjectDeserializer.deserializeUnsafe()

            when (event.type) {
                "invoice.payment_failed" -> {
                    val invoice = stripeObject as Invoice
                    val invoiceId = invoice.id

                    val userEmail = invoice.customerEmail
                    if (userEmail == null) {
                        call.application.environment.log.info("No user email found for invoice $invoiceId")
                        return@post
                    }

                    val user = userService.readByEmail(userEmail)
                    if (user == null) {
                        call.application.environment.log.info("No user found for invoice $invoiceId")
                        return@post
                    }

                    userService.update(user.id as Int, user.copy(role = "free"));
                }
                "customer.subscription.deleted" -> {

                }
                "invoice.paid" -> {
                    val invoice = stripeObject as Invoice
                    val invoiceId = invoice.id

                    val userEmail = invoice.customerEmail
                    if (userEmail == null) {
                        call.application.environment.log.info("No user email found for invoice $invoiceId")
                        return@post
                    }

                    val user = userService.readByEmail(userEmail)
                    if (user == null) {
                        call.application.environment.log.info("No user found for invoice $invoiceId")
                        return@post
                    }

                    val tier = when (invoice.lines.data.first().price.id) {
                        STRIPE_TIER_1_PRICE_ID -> "tier1"
                        STRIPE_TIER_2_PRICE_ID -> "tier2"
                        STRIPE_TIER_3_PRICE_ID -> "tier3"
                        else -> "free"
                    }

                    userService.update(user.id as Int, user.copy(role = tier));
                }
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}
