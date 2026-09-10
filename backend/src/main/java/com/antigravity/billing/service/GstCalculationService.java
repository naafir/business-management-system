package com.antigravity.billing.service;

import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;

public interface GstCalculationService {
    GstCalculationResult calculate(GstCalculationRequest request);
    boolean isInterState(String businessStateCode, String placeOfSupplyStateCode);
}
