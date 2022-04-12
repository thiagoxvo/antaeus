package io.pleo.antaeus.core.exceptions

class FailedPaymentException(invoiceId: Int) :
    Exception("Payment failed for invoice '$invoiceId'")
