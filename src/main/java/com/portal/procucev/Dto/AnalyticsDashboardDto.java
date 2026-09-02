package com.portal.procucev.Dto;

import java.util.List;

public class AnalyticsDashboardDto {
    private Object metrics;
    private List<Object> sources;
    private List<Object> lifecycleStages;
    private List<Object> noQuoteAlerts;

    public AnalyticsDashboardDto() {}

    public AnalyticsDashboardDto(Object metrics, List<Object> sources, List<Object> lifecycleStages, List<Object> noQuoteAlerts) {
        this.metrics = metrics;
        this.sources = sources;
        this.lifecycleStages = lifecycleStages;
        this.noQuoteAlerts = noQuoteAlerts;
    }

    public Object getMetrics() { return metrics; }
    public void setMetrics(Object metrics) { this.metrics = metrics; }

    public List<Object> getSources() { return sources; }
    public void setSources(List<Object> sources) { this.sources = sources; }

    public List<Object> getLifecycleStages() { return lifecycleStages; }
    public void setLifecycleStages(List<Object> lifecycleStages) { this.lifecycleStages = lifecycleStages; }

    public List<Object> getNoQuoteAlerts() { return noQuoteAlerts; }
    public void setNoQuoteAlerts(List<Object> noQuoteAlerts) { this.noQuoteAlerts = noQuoteAlerts; }
}
