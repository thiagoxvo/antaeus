package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    fun generateInvoices(): List<Invoice> {
        // TODO - This filter should be done at the DB level
        // fetch all unpaid invoices
        val unpaidInvoices = invoiceService.fetchAll().filter { it.status == InvoiceStatus.PENDING }

        for (invoice in unpaidInvoices) {
            if(paymentProvider.charge(invoice)) {
                //update invoice to paid
            }
        }

        return unpaidInvoices;
    }
}
