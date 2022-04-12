package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {
    private val paidInvoice = Invoice(
        status = InvoiceStatus.PAID,
        amount = Money(BigDecimal.valueOf(100), Currency.GBP),
        customerId = 1,
        id = 1
    )
    private val pendingInvoice = Invoice(
        status = InvoiceStatus.PENDING,
        amount = Money(BigDecimal.valueOf(100), Currency.GBP),
        customerId = 1,
        id = 2
    )

    private val invoiceService = mockk<InvoiceService> {
        every { fetchAll() } returns listOf(paidInvoice, pendingInvoice)
    }

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    private val billingService = BillingService(invoiceService = invoiceService, paymentProvider = paymentProvider)

    @Test
    fun `return only unpaid invoices`() {
        assertThat(billingService.generateInvoices().size).isEqualTo(1)
    }
}