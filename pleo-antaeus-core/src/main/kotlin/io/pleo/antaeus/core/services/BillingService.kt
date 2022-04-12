package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.FailedPaymentException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.lang.String.format

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    fun processInvoices() {
        // Simplistic approach to load all pending invoices and filtering in memory, this should be done at the DB level.
        val unpaidInvoices = invoiceService.fetchAll().filter { it.status == InvoiceStatus.PENDING }

        if (unpaidInvoices.isEmpty()) {
            logger.info { "No pending invoices to be billed" }
        }

        for (invoice in unpaidInvoices) {
            try {
                // This implementation is very naive, in real life we would use a more robust payment provider client,
                // which would implement some resilience strategies, like retry if failed. (check if possible to retry)

                // Another problem here is working in multi node environment, we would need somehow
                // implement a distributed lock otherwise we might double charge customers.
                if (paymentProvider.charge(invoice)) {
                    // There's another point that can fail here, as we are fetching all PENDING invoices, the payment
                    // could succeed but the update not. If we don't change the status, this payment will be tried again
                    // in another loop.
                    invoiceService.update(invoice.copy(status = InvoiceStatus.PAID))
                    logger.info { format("Invoice payment succeed id=[%d]", invoice.id) }
                } else {
                    // Here this invoice could be moved to a queue/topic using backoff retry as the customer can
                    // receive funds on their balance later during the day.
                    // And after a number X of attempts we could move to another queue/topic to charge manually.
                    logger.info { format("Invoice payment failed id=[%d]", invoice.id) }
                    throw FailedPaymentException(invoice.id)
                }
            } catch (e: CustomerNotFoundException) {
                // This is probably invalid data that needs to be checked manually
                logger.info { format("Invoice payment failed id=[%d] - error=[%s]", invoice.id, e.message) }
                throw FailedPaymentException(invoice.id)
            } catch (e: CurrencyMismatchException) {
                // This is probably invalid data that needs to be checked manually
                logger.info { format("Invoice payment failed id=[%d] - error=[%s]", invoice.id, e.message) }
                throw FailedPaymentException(invoice.id)
            } catch (e: NetworkException) {
                // A more resilient payment client implementation would retry a few times
                // (be careful with idempotency here) before failing completely .
                logger.info { format("Invoice payment failed id=[%d] - error=[%s]", invoice.id, e.message) }
                throw FailedPaymentException(invoice.id)
            }
        }
    }
}
