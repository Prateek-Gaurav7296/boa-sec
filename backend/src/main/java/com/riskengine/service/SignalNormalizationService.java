package com.riskengine.service;

import com.riskengine.dto.IframeSignals;
import com.riskengine.dto.NormalizedSignals;
import com.riskengine.dto.SignalRequest;
import org.springframework.stereotype.Service;

/**
 * Normalizes raw client signals for rule-based (and future ML) consumption.
 * Boolean → 0/1, iframe counts from iframeSignals, click interval &lt; 50ms → rapid-click flag.
 */
@Service
public class SignalNormalizationService {

    private static final double RAPID_CLICK_THRESHOLD_MS = 50.0;

    public NormalizedSignals normalize(SignalRequest request) {
        int webdriver = booleanToInt(request.getWebdriverFlag());
        int fetchOverridden = booleanToInt(request.getFetchOverridden());
        int iframeHidden = getIframeCount(request, IframeSignals::getHidden);
        int iframeOffscreen = getIframeCount(request, IframeSignals::getOffscreen);
        int iframeCrossOrigin = getIframeCount(request, IframeSignals::getCrossOrigin);
        int rapidClicking = isRapidClicking(request.getClickIntervalAvg());

        return NormalizedSignals.builder()
                .webdriverFlag(webdriver)
                .fetchOverridden(fetchOverridden)
                .iframeHidden(iframeHidden)
                .iframeOffscreen(iframeOffscreen)
                .iframeCrossOrigin(iframeCrossOrigin)
                .rapidClicking(rapidClicking)
                .build();
    }

    private static int getIframeCount(SignalRequest request, java.util.function.ToIntFunction<IframeSignals> getter) {
        IframeSignals s = request.getIframeSignals();
        return s == null ? 0 : Math.max(0, getter.applyAsInt(s));
    }

    private static int booleanToInt(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private static int isRapidClicking(Double clickIntervalAvg) {
        if (clickIntervalAvg == null) return 0;
        return clickIntervalAvg < RAPID_CLICK_THRESHOLD_MS ? 1 : 0;
    }
}
