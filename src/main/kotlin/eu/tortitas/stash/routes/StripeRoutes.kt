package eu.tortitas.stash.routes

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Invoice
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent.PaymentMethodOptions
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



fun Route.stripeRoute(application: Application) {
    val userService = application.provideUserService()
    val linkService = application.provideLinkService()
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

                val amount = when (call.request.queryParameters["tier"]) {
                    "1" -> 100L
                    "2" -> 300L
                    "3" -> 600L
                    else -> null
                }
                if (amount == null) {
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

                val paramsPaymentIntent = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("eur")
                    .setCustomer(stripeCustomerId)
                    .build();
                val intent = PaymentIntent.create(paramsPaymentIntent);

                call.respond(HttpStatusCode.OK, intent.clientSecret)
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
                "invoice.overdue" -> {
                    val invoice = stripeObject as Invoice
                    val invoiceId = invoice.id

                    val userId = invoice.customFields.find { it.name == "user_id" }?.value?.toIntOrNull()
                    if (userId == null) {
                        call.application.environment.log.info("No user id found for invoice $invoiceId")
                        return@post
                    }

                    val user = userService.read(userId)
                    if (user == null) {
                        call.application.environment.log.info("No user found for invoice $invoiceId")
                        return@post
                    }

                    userService.update(userId, user.copy(role = "banned"));
                }
                "invoice.paid" -> {
                    val invoice = stripeObject as Invoice
                    val invoiceId = invoice.id

                    val userId = invoice.customFields.find { it.name == "user_id" }?.value?.toIntOrNull()
                    if (userId == null) {
                        call.application.environment.log.info("No user id found for invoice $invoiceId")
                        return@post
                    }

                    val user = userService.read(userId)
                    if (user == null) {
                        call.application.environment.log.info("No user found for invoice $invoiceId")
                        return@post
                    }

                    val tier = when (invoice.amountPaid) {
                        100L -> "tier1"
                        300L -> "tier2"
                        600L -> "tier3"
                        else -> "banned"
                    }

                    userService.update(userId, user.copy(role = tier));
                }
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}
