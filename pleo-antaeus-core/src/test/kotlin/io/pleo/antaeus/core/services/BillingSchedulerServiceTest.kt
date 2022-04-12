package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BillingSchedulerServiceTest {

    private val billingService = mockk<BillingService>() {
        every { processInvoices() }
    }

    private val billingSchedulerService = BillingSchedulerService(billingService)

    @Test
    fun `should trigger billing processing on the processing day (default first day of the month)`() {
        val currentDay = LocalDate.now().dayOfMonth

        billingSchedulerService.start(processingDay = currentDay)
        verify(timeout = 10000) { billingService.processInvoices() }
    }
}