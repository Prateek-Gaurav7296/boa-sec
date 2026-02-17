(function (global) {
    'use strict';
    var clickTimestamps = [];
    var lastClickTime = 0;
    var MAX_CLICKS = 20;

    /** Allowed organization hostnames (page and iframe URLs must be from these to avoid "not from org" flag). Override via window.RiskAgentOrgHosts if needed. */
    var ORG_HOSTS = (typeof global !== 'undefined' && global.RiskAgentOrgHosts && Array.isArray(global.RiskAgentOrgHosts))
        ? global.RiskAgentOrgHosts
        : ['localhost', '127.0.0.1'];

    if (typeof document !== 'undefined') {
        document.addEventListener('click', function () {
            var now = Date.now();
            if (lastClickTime > 0) {
                clickTimestamps.push(now - lastClickTime);
                if (clickTimestamps.length > MAX_CLICKS) clickTimestamps.shift();
            }
            lastClickTime = now;
        }, true);
    }

    /**
     * Security-focused iframe detection for clickjacking, hidden injection,
     * cross-origin content, off-screen rendering, and zero-dimension frames.
     * Browser-native only; defensive checks throughout.
     *
     * @returns { { totalIframes: number, suspiciousIframes: number, hiddenCount: number, offscreenCount: number, crossOriginCount: number, notFromOrgCount: number } }
     */
    function detectSuspiciousIframes() {
        var result = {
            totalIframes: 0,
            suspiciousIframes: 0,
            hiddenCount: 0,
            offscreenCount: 0,
            crossOriginCount: 0,
            notFromOrgCount: 0
        };
        try {
            if (typeof document === 'undefined' || !document.getElementsByTagName) return result;
            var iframes = document.getElementsByTagName('iframe');
            result.totalIframes = iframes ? iframes.length : 0;
            if (!iframes || iframes.length === 0) return result;

            var currentHost = '';
            try {
                if (typeof window !== 'undefined' && window.location && window.location.hostname) {
                    currentHost = window.location.hostname || '';
                }
            } catch (e) { /* ignore */ }

            function isHostFromOrg(host) {
                if (!host) return false;
                host = host.toLowerCase();
                for (var k = 0; k < ORG_HOSTS.length; k++) {
                    if (ORG_HOSTS[k] && host === ORG_HOSTS[k].toLowerCase()) return true;
                }
                return false;
            }

            for (var i = 0; i < iframes.length; i++) {
                var iframe = iframes[i];
                var isHidden = false;
                var isOffscreen = false;
                var isCrossOrigin = false;
                var isNotFromOrg = false;

                try {
                    // --- A) Hidden iframe detection ---
                    // Inline style: display none or visibility hidden
                    if (iframe.style) {
                        if (iframe.style.display === 'none' || iframe.style.visibility === 'hidden') {
                            isHidden = true;
                        }
                        // opacity 0 effectively hides the frame
                        if (iframe.style.opacity === '0' || iframe.style.opacity === '0.0') {
                            isHidden = true;
                        }
                    }
                    // Zero dimensions (offset)
                    if (iframe.offsetWidth === 0 || iframe.offsetHeight === 0) {
                        isHidden = true;
                    }
                    // getBoundingClientRect for actual rendered size (handles transforms)
                    try {
                        var rect = iframe.getBoundingClientRect ? iframe.getBoundingClientRect() : null;
                        if (rect && (rect.width === 0 || rect.height === 0)) {
                            isHidden = true;
                        }
                    } catch (e) { /* ignore */ }
                    // Computed style opacity (defensive: only if getComputedStyle exists)
                    try {
                        if (typeof window !== 'undefined' && window.getComputedStyle) {
                            var computed = window.getComputedStyle(iframe);
                            if (computed && computed.opacity === '0') {
                                isHidden = true;
                            }
                        }
                    } catch (e) { /* ignore */ }

                    // --- B) Off-screen iframe detection ---
                    // rect outside viewport indicates possible clickjacking / overlay abuse
                    try {
                        var r = iframe.getBoundingClientRect ? iframe.getBoundingClientRect() : null;
                        if (r && typeof window !== 'undefined' && window.innerWidth != null && window.innerHeight != null) {
                            var w = window.innerWidth;
                            var h = window.innerHeight;
                            if (r.left < 0 || r.top < 0 || r.right > w || r.bottom > h) {
                                isOffscreen = true;
                            }
                        }
                    } catch (e) { /* ignore */ }

                    // --- C) Cross-origin and D) Not-from-org iframe detection ---
                    try {
                        if (iframe.src) {
                            var src = iframe.src;
                            var iframeHost = '';
                            try {
                                var a = document.createElement('a');
                                a.href = src;
                                iframeHost = a.hostname || '';
                            } catch (e) {
                                try {
                                    var url = typeof URL !== 'undefined' ? new URL(src) : null;
                                    if (url) iframeHost = url.hostname || '';
                                } catch (e2) { /* ignore */ }
                            }
                            // Different hostname (including empty vs non-empty) indicates cross-origin
                            if (iframeHost !== currentHost) {
                                isCrossOrigin = true;
                            }
                            // Iframe URL host is not in the allowed org list → flag as not from org
                            if (iframeHost && !isHostFromOrg(iframeHost)) {
                                isNotFromOrg = true;
                            }
                        }
                    } catch (e) { /* iframe.src null or access error */ }

                    if (isHidden) result.hiddenCount++;
                    if (isOffscreen) result.offscreenCount++;
                    if (isCrossOrigin) result.crossOriginCount++;
                    if (isNotFromOrg) result.notFromOrgCount++;
                    if (isHidden || isOffscreen || isCrossOrigin || isNotFromOrg) { result.suspiciousIframes++; }
                } catch (e) {
                    /* per-iframe failure: skip, do not crash */
                }
            }
            return result;
        } catch (e) {
            return result;
        }
    }

    function getWebdriver() {
        try { return !!(typeof navigator !== 'undefined' && navigator.webdriver); } catch (e) { return false; }
    }
    function getFetchOverridden() {
        try {
            if (typeof window === 'undefined' || !window.fetch) return false;
            var s = Function.prototype.toString.call(window.fetch);
            return s.indexOf('[native code]') === -1 && s.indexOf('native') === -1;
        } catch (e) { return false; }
    }
    function getScreen() {
        try {
            if (typeof screen !== 'undefined') return { width: screen.width || 0, height: screen.height || 0 };
            return { width: 0, height: 0 };
        } catch (e) { return { width: 0, height: 0 }; }
    }
    function getTimezone() {
        try {
            if (typeof Intl !== 'undefined' && Intl.DateTimeFormat)
                return Intl.DateTimeFormat().resolvedOptions().timeZone || '';
            return '';
        } catch (e) { return ''; }
    }
    function getClickIntervalAvg() {
        if (clickTimestamps.length === 0) return null;
        var sum = 0;
        for (var i = 0; i < clickTimestamps.length; i++) sum += clickTimestamps[i];
        return sum / clickTimestamps.length;
    }

    /** Current page origin (e.g. https://app.mycompany.com:8080). Used to flag if page URL is not from org. */
    function getPageOrigin() {
        try {
            if (typeof window === 'undefined' || !window.location) return '';
            var loc = window.location;
            return (loc.protocol || '') + '//' + (loc.hostname || '') + (loc.port ? ':' + loc.port : '');
        } catch (e) { return ''; }
    }

    /** True if the current page's hostname is not in the allowed org host list (possible phishing / wrong site). */
    function isPageOriginNotFromOrg() {
        try {
            if (typeof window === 'undefined' || !window.location || !window.location.hostname) return false;
            var host = (window.location.hostname || '').toLowerCase();
            for (var i = 0; i < ORG_HOSTS.length; i++) {
                if (ORG_HOSTS[i] && host === ORG_HOSTS[i].toLowerCase()) return false;
            }
            return true;
        } catch (e) { return false; }
    }

    function capture() {
        var screenD = getScreen();
        var iframeSignals = detectSuspiciousIframes();
        return {
            webdriverFlag: getWebdriver(),
            pageOrigin: getPageOrigin(),
            pageOriginNotFromOrg: isPageOriginNotFromOrg(),
            iframeSignals: {
                total: iframeSignals.totalIframes,
                suspicious: iframeSignals.suspiciousIframes,
                hidden: iframeSignals.hiddenCount,
                offscreen: iframeSignals.offscreenCount,
                crossOrigin: iframeSignals.crossOriginCount,
                notFromOrg: iframeSignals.notFromOrgCount
            },
            fetchOverridden: getFetchOverridden(),
            userAgent: typeof navigator !== 'undefined' ? (navigator.userAgent || '') : '',
            screenWidth: screenD.width,
            screenHeight: screenD.height,
            timezone: getTimezone(),
            clickIntervalAvg: getClickIntervalAvg(),
            referrerUrl: (typeof document !== 'undefined' && document.referrer) ? document.referrer : ''
        };
    }
    function buildPayload(sessionId, userId, signals) {
        return {
            sessionId: sessionId || '',
            userId: userId || '',
            webdriverFlag: signals.webdriverFlag,
            pageOrigin: (signals && signals.pageOrigin) || getPageOrigin(),
            pageOriginNotFromOrg: !!(signals && signals.pageOriginNotFromOrg),
            referrerUrl: (signals && signals.referrerUrl) !== undefined ? signals.referrerUrl : ((typeof document !== 'undefined' && document.referrer) ? document.referrer : ''),
            iframeSignals: signals.iframeSignals || { total: 0, suspicious: 0, hidden: 0, offscreen: 0, crossOrigin: 0, notFromOrg: 0 },
            fetchOverridden: signals.fetchOverridden,
            userAgent: signals.userAgent,
            screenWidth: signals.screenWidth,
            screenHeight: signals.screenHeight,
            timezone: signals.timezone,
            clickIntervalAvg: signals.clickIntervalAvg
        };
    }
    function captureAndBuildPayload(sessionId, userId) {
        return buildPayload(sessionId, userId, capture());
    }

    global.RiskAgent = {
        detectSuspiciousIframes: detectSuspiciousIframes,
        capture: capture,
        buildPayload: buildPayload,
        captureAndBuildPayload: captureAndBuildPayload
    };
})(typeof window !== 'undefined' ? window : this);
