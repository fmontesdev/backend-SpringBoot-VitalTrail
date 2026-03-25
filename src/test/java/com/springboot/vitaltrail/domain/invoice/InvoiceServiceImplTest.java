package com.springboot.vitaltrail.domain.invoice;

import com.springboot.vitaltrail.api.invoice.InvoiceDto;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;
import com.springboot.vitaltrail.domain.payment.StripeDataService;

import com.stripe.exception.StripeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    private static final String CUSTOMER_ID = "cus_test123";

    @Mock
    private StripeDataService stripeDataService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    @Test
    void getInvoices_devuelveLista() throws StripeException {
        // given
        List<InvoiceDto> facturasMock = List.of(
            InvoiceDto.builder()
                .id("in_001")
                .number("INV-0001")
                .status("paid")
                .amountTotal(999L)
                .currency("eur")
                .created(1700000000L)
                .invoiceUrl("https://invoice.stripe.com/001")
                .invoicePdf("https://invoice.stripe.com/001.pdf")
                .build()
        );
        doReturn(facturasMock).when(stripeDataService).getCustomerInvoices(CUSTOMER_ID);

        // when
        List<InvoiceDto> result = invoiceService.getInvoices(CUSTOMER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("in_001");
        assertThat(result.get(0).getAmountTotal()).isEqualTo(999L);
    }

    @Test
    void getInvoices_stripeExcepcion_lanzaAppException() throws StripeException {
        // given
        doThrow(new com.stripe.exception.ApiException("Error de Stripe", "req_test", "500", 500, null))
                .when(stripeDataService).getCustomerInvoices(CUSTOMER_ID);

        // when / then
        assertThatThrownBy(() -> invoiceService.getInvoices(CUSTOMER_ID))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getError()).isEqualTo(Error.STRIPE_ERROR));
    }
}
