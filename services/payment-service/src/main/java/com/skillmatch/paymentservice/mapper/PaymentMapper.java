package com.skillmatch.paymentservice.mapper;

import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.dto.response.InvoiceResponse;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;
import com.skillmatch.paymentservice.model.CommissionConfig;
import com.skillmatch.paymentservice.model.Invoice;
import com.skillmatch.paymentservice.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    TransactionResponse toResponse(Transaction transaction);

    @Mapping(source = "transaction.id", target = "transactionId")
    InvoiceResponse toResponse(Invoice invoice);

    CommissionConfigResponse toResponse(CommissionConfig commissionConfig);
}
