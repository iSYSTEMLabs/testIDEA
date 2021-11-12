package si.isystem.commons.utils.avg;

import java.util.Arrays;

public class ISysAverageDouble {
    private final double m_values[];
    private final int m_slotCount;

    private int m_sampleCount;
    private double m_sum;

    private double m_minValue, m_maxValue;

    public ISysAverageDouble() {
        this(0);
    }

    public ISysAverageDouble(int c) {
        m_slotCount = c;

        if (m_slotCount == 0) {
            m_values = null;
        } else {
            m_values = new double[m_slotCount];
            Arrays.fill(m_values, 0);
        }

        m_sampleCount = 0;
        m_sum = 0.0;
        m_minValue = Double.MAX_VALUE;
        m_maxValue = -Double.MAX_VALUE;
    }

    public void reset() {
        if (m_slotCount > 0) {
            Arrays.fill(m_values, 0);
        }

        m_sampleCount = 0;
        m_sum = 0.0;
        m_minValue = Float.MAX_VALUE;
        m_maxValue = Float.MIN_VALUE;
    }

    public void addValue(double val) {
        if (m_slotCount == 0) {
            m_sum += val;
            m_sampleCount++;
        } else {
            int next = m_sampleCount % m_slotCount;

            m_sum -= m_values[next];
            m_sum += val;

            m_values[next] = val;

            m_sampleCount++;
        }

        m_minValue = m_minValue < val ? m_minValue : val;
        m_maxValue = m_maxValue > val ? m_maxValue : val;
    }

    public double getAverage() {
        if (m_sampleCount == 0) {
            return 0;
        }
        if (m_slotCount == 0) {
            return m_sum / m_sampleCount;
        } else {
            return m_sum / (m_sampleCount < m_slotCount ? m_sampleCount : m_slotCount);
        }
    }

    public double getSum() {
        return m_sum;
    }

    public long getSampleCount() {
        return m_sampleCount;
    }

    public double getMinValue() {
        return m_minValue;
    }

    public double getMaxValue() {
        return m_maxValue;
    }
}
