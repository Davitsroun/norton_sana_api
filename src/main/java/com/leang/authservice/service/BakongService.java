package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.BakongRequest;
import com.leang.authservice.model.dto.request.CheckTransactionRequest;
import com.leang.authservice.model.dto.response.BakongResponse;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

public interface BakongService {

    KHQRResponse<KHQRData> generateQR(BakongRequest request);

    byte[] getQRImage(KHQRData qr);

    BakongResponse checkTransactionByMD5(CheckTransactionRequest request);
}
