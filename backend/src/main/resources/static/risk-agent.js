'use strict';

async function sha256(str) {
  const buffer = new TextEncoder().encode(str);
  const hash = await crypto.subtle.digest("SHA-256", buffer);
  return Array.from(new Uint8Array(hash))
    .map(b => b.toString(16).padStart(2, "0"))
    .join("");
}

function collectFastSignals() {
  return {
    userAgent: navigator.userAgent,
    platform: navigator.platform,
    language: navigator.language,
    languages: navigator.languages,
    webdriver: navigator.webdriver,
    hardwareConcurrency: navigator.hardwareConcurrency,
    deviceMemory: navigator.deviceMemory,
    cookieEnabled: navigator.cookieEnabled,
    doNotTrack: navigator.doNotTrack,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    screen: {
      width: screen.width,
      height: screen.height,
      colorDepth: screen.colorDepth,
      pixelRatio: window.devicePixelRatio
    },
    referrer: document.referrer,
    origin: location.origin
  };
}

async function collectCanvasFingerprint() {
  try {
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");
    if (!ctx) return null;

    ctx.textBaseline = "top";
    ctx.font = "14px Arial";
    ctx.fillStyle = "#f60";
    ctx.fillRect(125, 1, 62, 20);
    ctx.fillStyle = "#069";
    ctx.fillText("RiskEngineFingerprint", 2, 15);

    return canvas.toDataURL();
  } catch (e) {
    return null;
  }
}

async function collectWebGLFingerprint() {
  try {
    const canvas = document.createElement("canvas");
    const gl = canvas.getContext("webgl") || canvas.getContext("experimental-webgl");
    if (!gl) return null;

    const debugInfo = gl.getExtension("WEBGL_debug_renderer_info");
    const vendor = debugInfo ? gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) : gl.getParameter(gl.VENDOR);
    const renderer = debugInfo ? gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER);
    const shadingLanguageVersion = gl.getParameter(gl.SHADING_LANGUAGE_VERSION);
    const extensions = gl.getSupportedExtensions();
    const extensionsSorted = extensions ? extensions.slice().sort() : [];

    return {
      VENDOR: vendor,
      RENDERER: renderer,
      SHADING_LANGUAGE_VERSION: shadingLanguageVersion,
      EXTENSIONS: extensionsSorted
    };
  } catch (e) {
    return null;
  }
}

async function collectAudioFingerprint() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return null;

    const context = new AudioContext();
    const offlineContext = new OfflineAudioContext(1, 44100, 44100);
    const oscillator = offlineContext.createOscillator();
    oscillator.type = "triangle";
    oscillator.frequency.value = 10000;
    oscillator.connect(offlineContext.destination);
    oscillator.start(0);
    const buffer = await offlineContext.startRendering();
    oscillator.stop();
    context.close();

    const channelData = buffer.getChannelData(0);
    const samples = channelData ? Array.from(channelData).slice(0, 100) : [];
    return samples.map(s => String(s)).join("");
  } catch (e) {
    return null;
  }
}

function detectFonts() {
  const baseFonts = ["monospace", "sans-serif", "serif"];
  const testFonts = [
    "Arial", "Arial Black", "Comic Sans MS", "Courier New", "Georgia",
    "Impact", "Times New Roman", "Trebuchet MS", "Verdana", "Lucida Console",
    "Tahoma", "Palatino Linotype", "Lucida Sans Unicode", "MS Gothic"
  ];
  const testString = "mmmmmmmmmmlli";
  const testSize = "72px";
  const canvas = document.createElement("canvas");
  const ctx = canvas.getContext("2d");
  if (!ctx) return [];

  const baseWidths = {};
  for (let i = 0; i < baseFonts.length; i++) {
    ctx.font = testSize + " " + baseFonts[i];
    baseWidths[baseFonts[i]] = ctx.measureText(testString).width;
  }

  const detected = [];
  for (let i = 0; i < testFonts.length; i++) {
    let detectedForFont = false;
    for (let j = 0; j < baseFonts.length; j++) {
      ctx.font = testSize + " '" + testFonts[i] + "', " + baseFonts[j];
      const w = ctx.measureText(testString).width;
      if (w !== baseWidths[baseFonts[j]]) {
        detectedForFont = true;
        break;
      }
    }
    if (detectedForFont) detected.push(testFonts[i]);
  }
  return detected;
}

function detectAutomation() {
  return {
    webdriver: !!navigator.webdriver,
    pluginsLength: navigator.plugins ? navigator.plugins.length : 0,
    mimeTypesLength: navigator.mimeTypes ? navigator.mimeTypes.length : 0,
    hasChrome: "chrome" in window,
    hasWebdriverScriptFn: "__webdriver_script_fn" in document
  };
}

function detectFunctionTampering() {
  try {
    const fnStr = Function.prototype.toString.toString();
    const functionTampered = fnStr.indexOf("[native code]") === -1;
    return functionTampered;
  } catch (e) {
    return true;
  }
}

function detectIframeMismatch() {
  return new Promise(function (resolve) {
    try {
      const iframe = document.createElement("iframe");
      iframe.style.display = "none";
      iframe.src = "about:blank";
      document.body.appendChild(iframe);

      function check() {
        try {
          const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
          const iframeNav = iframe.contentWindow ? iframe.contentWindow.navigator : iframeDoc.defaultView.navigator;
          const mainUA = navigator.userAgent;
          const mainPlatform = navigator.platform;
          const iframeUA = iframeNav.userAgent;
          const iframePlatform = iframeNav.platform;
          const iframeMismatch = mainUA !== iframeUA || mainPlatform !== iframePlatform;
          if (iframe.parentNode) iframe.parentNode.removeChild(iframe);
          resolve(iframeMismatch);
        } catch (e) {
          if (iframe.parentNode) iframe.parentNode.removeChild(iframe);
          resolve(true);
        }
      }

      if (iframe.contentDocument && iframe.contentDocument.readyState === "complete") {
        check();
      } else {
        iframe.onload = check;
        setTimeout(check, 500);
      }
    } catch (e) {
      resolve(true);
    }
  });
}

function testStorage() {
  try {
    const key = "_risk_agent_test_";
    const val = "1";

    try {
      localStorage.setItem(key, val);
      const lr = localStorage.getItem(key);
      localStorage.removeItem(key);
      if (lr !== val) return false;
    } catch (e) {
      return false;
    }

    try {
      sessionStorage.setItem(key, val);
      const sr = sessionStorage.getItem(key);
      sessionStorage.removeItem(key);
      if (sr !== val) return false;
    } catch (e) {
      return false;
    }

    try {
      document.cookie = key + "=" + val + "; path=/";
      const hasCookie = document.cookie.indexOf(key + "=") !== -1;
      document.cookie = key + "=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
      if (!hasCookie) return false;
    } catch (e) {
      return false;
    }

    return true;
  } catch (e) {
    return false;
  }
}

function testCSP() {
  try {
    const script = document.createElement("script");
    script.textContent = "void 0;";
    document.head.appendChild(script);
    script.parentNode.removeChild(script);
    return false;
  } catch (e) {
    return true;
  }
}

function scanIframes() {
  const result = { total: 0, suspicious: 0, hidden: 0, offscreen: 0, crossOrigin: 0, notFromOrg: 0 };
  try {
    const iframes = document.querySelectorAll("iframe");
    result.total = iframes.length;
    if (iframes.length === 0) return result;

    const pageOrigin = (typeof location !== "undefined" && location.origin) ? location.origin : "";
    const allowedHosts = (typeof window !== "undefined" && window.RiskAgentOrgHosts) ? window.RiskAgentOrgHosts : [];

    function hostFromUrl(url) {
      if (!url || typeof url !== "string") return null;
      try {
        const a = document.createElement("a");
        a.href = url;
        return a.hostname ? a.hostname.toLowerCase() : null;
      } catch (e) {
        return null;
      }
    }

    function isHostAllowed(host) {
      if (!host) return false;
      host = host.toLowerCase();
      for (let i = 0; i < allowedHosts.length; i++) {
        if (String(allowedHosts[i]).toLowerCase() === host) return true;
      }
      return false;
    }

    const vw = typeof window !== "undefined" ? window.innerWidth || 0 : 0;
    const vh = typeof window !== "undefined" ? window.innerHeight || 0 : 0;

    for (let i = 0; i < iframes.length; i++) {
      const iframe = iframes[i];
      let hidden = false;
      let offscreen = false;
      let crossOrigin = false;
      let notFromOrg = false;

      try {
        const style = window.getComputedStyle(iframe);
        const display = (style && style.display) ? style.display : "";
        const visibility = (style && style.visibility) ? style.visibility : "";
        const opacity = (style && style.opacity) ? parseFloat(style.opacity) : 1;
        const rect = iframe.getBoundingClientRect();
        const w = rect ? rect.width : 0;
        const h = rect ? rect.height : 0;
        const left = rect ? rect.left : 0;
        const top = rect ? rect.top : 0;

        if (display === "none" || visibility === "hidden" || (opacity !== undefined && opacity <= 0) || (w <= 0 && h <= 0)) {
          hidden = true;
        }
        if (left + w < 0 || top + h < 0 || left >= vw || top >= vh) {
          offscreen = true;
        }

        const src = iframe.src || "";
        if (src && pageOrigin) {
          try {
            const iframeOrigin = src.split("/").slice(0, 3).join("/");
            if (iframeOrigin && iframeOrigin !== pageOrigin) {
              crossOrigin = true;
            }
          } catch (e) {
            crossOrigin = true;
          }
          const host = hostFromUrl(src);
          if (host && allowedHosts.length > 0 && !isHostAllowed(host)) {
            notFromOrg = true;
          }
        }
      } catch (e) {
        hidden = true;
      }

      if (hidden) result.hidden++;
      if (offscreen) result.offscreen++;
      if (crossOrigin) result.crossOrigin++;
      if (notFromOrg) result.notFromOrg++;
      if (hidden || offscreen || crossOrigin || notFromOrg) result.suspicious++;
    }
  } catch (e) {}
  return result;
}

async function collectAllSignals() {
  const stage1 = collectFastSignals();

  const canvas = await collectCanvasFingerprint();
  const webgl = await collectWebGLFingerprint();
  const audio = await collectAudioFingerprint();
  const fonts = detectFonts();

  const stage2 = {
    canvasHash: canvas ? await sha256(canvas) : null,
    webglHash: webgl ? await sha256(JSON.stringify(webgl)) : null,
    audioHash: audio ? await sha256(audio) : null,
    fontsHash: fonts ? await sha256(JSON.stringify(fonts)) : null
  };

  const stage3 = {
    automation: detectAutomation(),
    functionTampered: detectFunctionTampering(),
    iframeMismatch: await detectIframeMismatch(),
    storageWorks: testStorage(),
    cspRestricted: testCSP()
  };

  const iframeSignals = scanIframes();

  return {
    timestamp: Date.now(),
    stage1,
    stage2,
    stage3,
    iframeSignals
  };
}

function buildPayload(sessionId, userId, signals) {
  const payload = Object.assign({}, signals);
  if (sessionId != null) payload.sessionId = sessionId;
  if (userId != null) payload.userId = userId;
  return payload;
}

async function captureAndBuildPayload(sessionId, userId) {
  const signals = await collectAllSignals();
  return buildPayload(sessionId, userId, signals);
}

async function sendToBackend(payload) {
  await fetch("/risk/collect", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
}

(function init() {
  if (typeof document === "undefined" || typeof window === "undefined") return;
  function run() {
    var sessionId = null;
    var userId = null;
    try {
      if (typeof sessionStorage !== "undefined") {
        sessionId = sessionStorage.getItem("sessionId");
        userId = sessionStorage.getItem("userId");
      }
    } catch (e) {}
    collectAllSignals().then(function (signals) {
      var payload = buildPayload(sessionId, userId, signals);
      sendToBackend(payload);
    });
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", run);
  } else {
    run();
  }
})();

window.RiskAgent = {
  collectAllSignals: collectAllSignals,
  captureAndBuildPayload: captureAndBuildPayload,
  buildPayload: buildPayload,
  sendToBackend: sendToBackend
};
