package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.FailedPaymentException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        amount = Money(BigDecimal.valueOf(200), Currency.EUR),
        customerId = 1,
        id = 2
    )

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetchAll() } returns listOf(paidInvoice, pendingInvoice)
        every { update(any()) } returns pendingInvoice
    }

    private val billingService = BillingService(invoiceService = invoiceService, paymentProvider = paymentProvider)

    @Test
    fun `should charge the pending invoice`() {
        billingService.processInvoices()
        verify(exactly = 1) { paymentProvider.charge(pendingInvoice) }
    }

    @Test
    fun `should not charge the paid invoice`() {
        billingService.processInvoices()

        verify(exactly = 0) { paymentProvider.charge(paidInvoice) }
    }

    @Test
    fun `will throw if customer have insufficient balance`() {
        every { paymentProvider.charge(any()) } returns false

        assertThrows<FailedPaymentException> {
            billingService.processInvoices()
        }
    }
}