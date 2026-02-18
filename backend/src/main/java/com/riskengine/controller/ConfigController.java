package com.riskengine.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Serves risk-agent config so the frontend uses the same allowed-hosts list as the backend.
 * Include &lt;script src="/config/risk-agent-config.js"&gt;&lt;/script&gt; before risk-agent.js.
 */
@RestController
@RequestMapping("/config")
public class ConfigController {

    @Value("${risk.engine.allowed-hosts:localhost,127.0.0.1}")
    private String allowedHostsConfig;

    @GetMapping(value = "/risk-agent-config.js", produces = "application/javascript")
    public String riskAgentConfigJs() {
        String hostsJson = Arrays.stream(allowedHostsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(h -> "\"" + h.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        return "window.RiskAgentOrgHosts=" + hostsJson + ";";
    }
}
