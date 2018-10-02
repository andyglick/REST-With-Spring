package com.baeldung.common.metric.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
class ActuatorMetricService implements IActuatorMetricService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Autowired
    private MetricReaderPublicMetrics publicMetrics;

    private final List<List<Integer>> statusMetricsByMinute;
    private final List<String> statusList;

    public ActuatorMetricService() {
        super();

        statusMetricsByMinute = new ArrayList<>();
        statusList = new ArrayList<>();
    }

    // API

    @Override
    public Object[][] getGraphData() {
        final int colCount = statusList.size() + 1;
        final int rowCount = statusMetricsByMinute.size() + 1;
        final Object[][] result = new Object[rowCount][colCount];

        final Date current = new Date();
        result[0][0] = "Time";
        int j = 1;
        for (final String httpStatus : statusList) {
            result[0][j] = httpStatus;
            j++;
        }

        List<Integer> minuteOfStatuses;
        List<Integer> last = new ArrayList<>();
        for (int i = 1; i < rowCount; i++) {
            minuteOfStatuses = statusMetricsByMinute.get(i - 1);
            result[i][0] = dateFormat.format(new Date(current.getTime() - (60000 * (rowCount - i))));
            for (j = 1; j <= minuteOfStatuses.size(); j++) {
                result[i][j] = minuteOfStatuses.get(j - 1) - (last.size() >= j ? last.get(j - 1) : 0);
            }
            while (j < colCount) {
                result[i][j] = 0;
                j++;
            }
            last = minuteOfStatuses;
        }
        return result;
    }

    // Non - API

    @Scheduled(fixedDelay = 60000)
    private void exportMetrics() {
        final List<Integer> lastMinuteStatuses = new ArrayList<>();

        initializeStatuses(lastMinuteStatuses);
        updateMetrics(lastMinuteStatuses);

        statusMetricsByMinute.add(lastMinuteStatuses);
    }

    private final void updateMetrics(final List<Integer> statusCount) {
        String status;
        int indexOfStatus;
        int old;

        for (final Metric<?> counterMetric : publicMetrics.metrics()) {
            if (counterMetric.getName().contains("counter.status.")) {
                status = counterMetric.getName().substring(15, 18);
                if (!statusList.contains(status)) {
                    statusList.add(status);
                    statusCount.add(0);
                }
                indexOfStatus = statusList.indexOf(status);
                old = statusCount.get(indexOfStatus) == null ? 0 : statusCount.get(indexOfStatus);
                statusCount.set(indexOfStatus, counterMetric.getValue().intValue() + old);
            }
        }
    }

    private final void initializeStatuses(final List<Integer> statusCount) {
        for (int i = 0; i < statusList.size(); i++) {
            statusCount.add(0);
        }
    }

}