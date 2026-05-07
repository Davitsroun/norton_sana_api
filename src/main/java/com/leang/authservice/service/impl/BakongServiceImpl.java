package com.leang.authservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.leang.authservice.model.dto.request.BakongRequest;
import com.leang.authservice.model.dto.request.CheckTransactionRequest;
import com.leang.authservice.model.dto.response.BakongResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Payment;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.PaymentRepository;
import com.leang.authservice.service.BakongService;
import com.leang.authservice.service.BakongTokenService;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.MerchantInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BakongServiceImpl implements BakongService {

    @Value("${bakong.account-id}")
    private String bakongAccountId;
    @Value("${bakong.base-url}")
    private String baseUrl;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper;
    private final BakongTokenService bakongTokenService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public KHQRResponse<KHQRData> generateQR(BakongRequest bakongRequest) {
        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setExpirationTimestamp(
                System.currentTimeMillis() + bakongRequest.expirationTimestamp() * 60 * 1000
        );
        merchantInfo.setBakongAccountId(bakongAccountId);
        merchantInfo.setMerchantId(bakongRequest.merchantId());
        merchantInfo.setAcquiringBank(bakongRequest.acquiringBank());
        merchantInfo.setCurrency(bakongRequest.currency());
        merchantInfo.setAmount(bakongRequest.amount());
        merchantInfo.setMerchantName(bakongRequest.merchantName());
        merchantInfo.setMerchantCity(bakongRequest.merchantCity());
        merchantInfo.setBillNumber(bakongRequest.billNumber());
        merchantInfo.setMobileNumber(bakongRequest.mobileNumber());
        merchantInfo.setStoreLabel(bakongRequest.storeLabel());
        merchantInfo.setUpiAccountInformation(bakongRequest.upiAccountInformation());
        merchantInfo.setMerchantAlternateLanguagePreference(bakongRequest.merchantAlternateLanguagePreference());
        merchantInfo.setMerchantNameAlternateLanguage(bakongRequest.merchantNameAlternateLanguage());
        merchantInfo.setMerchantCityAlternateLanguage(bakongRequest.merchantCityAlternateLanguage());
        merchantInfo.setPurposeOfTransaction(bakongRequest.purposeOfTransaction());
        merchantInfo.setTerminalLabel(bakongRequest.terminalLabel());
        return BakongKHQR.generateMerchant(merchantInfo);
    }

    @Override
    public byte[] getQRImage(KHQRData qr) {
        try {
            if (qr == null || qr.getQr() == null || qr.getQr().isBlank()) {
                return "Invalid QR data".getBytes(StandardCharsets.UTF_8);
            }

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = qrCodeWriter.encode(qr.getQr(), BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();

        } catch (WriterException e) {
            return "Error encoding QR data".getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ("Unexpected error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    @Transactional
    public BakongResponse checkTransactionByMD5(CheckTransactionRequest request) {
        String bearerToken = bakongTokenService.getToken();
        String url = baseUrl.replaceAll("/+$", "") + "/v1/check_transaction_by_md5";

        String responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("md5", request.md5()))
                .retrieve()
                .body(String.class);

        log.info("Data response from Bakong API: {}", responseBody);

        try {
            BakongResponse response = mapper.readValue(responseBody, BakongResponse.class);
            if (response.isSuccess() && request.orderId() != null) {
                updateOrderStatusAsPaid(request.orderId(), request.md5());
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Invalid upstream response", e);
        }
    }

    private void updateOrderStatusAsPaid(java.util.UUID orderId, String md5) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found for payment update"));
        order.setStatus("COMPLETED");
        orderRepository.save(order);

        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseGet(() -> Payment.builder()
                        .order(order)
                        .paymentMethod("BAKONG")
                        .build());
        payment.setPaymentStatus("PAID");
        payment.setTransactionId(md5);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
    }
}
