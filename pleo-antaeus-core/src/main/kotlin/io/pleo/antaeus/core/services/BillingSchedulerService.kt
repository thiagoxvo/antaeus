package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.FailedPaymentException
import mu.KotlinLogging
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

private val logger = KotlinLogging.logger {}

class BillingSchedulerService(private val billingService: BillingService) {

    private val name = "billing-scheduler"

    /*
    * Process invoices on processing day of the month.
    * The scheduler will keep running until the day of the month is reached.
    * */
    fun start(
        processingDay: Int = 12,
        startOn: Long = TimeUnit.SECONDS.toMillis(5), // 5 seconds after the server starts
        repeatOn: Long = TimeUnit.SECONDS.toMillis(10) // repeat every 10 seconds
    ) {
        logger.info { "Starting scheduler" }
        Timer(name).schedule(startOn, repeatOn) {
            val dayOfTheMonth = LocalDate.now().dayOfMonth
            if (dayOfTheMonth == processingDay) {
                logger.info { "Start processing invoices." }
                try {
                    billingService.processInvoices()
                } catch (e: FailedPaymentException) {
                    logger.error { "Payment failed. Retry will happen in a few seconds." }
                }
            } else {
                logger.info { "Today is not first day of the month, skip processing invoices." }
            }
        }
    }
}