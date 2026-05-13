package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Payment;
import com.leang.authservice.repository.PaymentRepository;
import com.leang.authservice.service.PaymentService;
import com.leang.authservice.service.PaymentSuccessSynchronizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentSuccessSynchronizer paymentSuccessSynchronizer;

    @Override
    @Transactional
    public Payment create(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        paymentSuccessSynchronizer.syncAfterPaymentPersisted(saved);
        return saved;
    }

    @Override
    @Transactional
    public Payment update(UUID id, Payment payment) {
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        existing.setOrder(payment.getOrder());
        existing.setPaymentMethod(payment.getPaymentMethod());
        existing.setPaymentStatus(payment.getPaymentStatus());
        existing.setTransactionId(payment.getTransactionId());
        existing.setPaidAt(payment.getPaidAt());
        Payment saved = paymentRepository.save(existing);
        paymentSuccessSynchronizer.syncAfterPaymentPersisted(saved);
        return saved;
    }

    @Override
    public void delete(UUID id) {
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        paymentRepository.delete(existing);
    }

    @Override
    public Payment getById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    @Override
    public ApiResponseWithPagination<Payment> getAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findAll(pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                paymentPage.getContent(),
                page,
                size,
                (int) paymentPage.getTotalElements()
        );
    }
}

