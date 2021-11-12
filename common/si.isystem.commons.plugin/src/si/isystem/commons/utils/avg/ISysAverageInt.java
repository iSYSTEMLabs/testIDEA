package si.isystem.commons.utils.avg;

import java.util.Arrays;

public class ISysAverageInt {
    private final int m_values[];
    private final int m_slotCount;

    private int m_sampleCount;
    private long m_sum;

    private long m_minValue, m_maxValue;

    public ISysAverageInt() {
        this(0);
    }

    public ISysAverageInt(int c) {
        m_slotCount = c;
        
        if (c == 0) {
            m_values = null;
        } else {
            m_values = new int[m_slotCount];
            Arrays.fill(m_values, 0);
        }

        m_sampleCount = 0;
        m_sum = 0;
        m_minValue = Long.MAX_VALUE;
        m_maxValue = Long.MIN_VALUE;
    }

    public void reset() {
        if (m_slotCount > 0) {
            Arrays.fill(m_values, 0);
        }

        m_sampleCount = 0;
        m_sum = 0L;
        m_minValue = Long.MAX_VALUE;
        m_maxValue = Long.MIN_VALUE;
    }

    public void addValue(int val) {
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

        if (val < m_minValue) {
            m_minValue = val;
        } else if (val > m_maxValue) {
            m_maxValue = val;
        }
    }

    public int getAverageInt() {
        if (m_slotCount == 0) {
            return (int)(m_sum / m_sampleCount);
        } else {
            return (int)(m_sum / (m_sampleCount < m_slotCount ? m_sampleCount : m_slotCount));
        }
    }

    public double getAverageDouble() {
        if (this.m_slotCount == 0) {
            return ((double) m_sum) / m_sampleCount;
        } else {
            return ((double) m_sum) / (m_sampleCount < m_slotCount ? m_sampleCount : m_slotCount);
        }
    }

    public long getSum() {
        return m_sum;
    }

    public long getSampleCount() {
        return m_sampleCount;
    }

    public long getMinValue() {
        return m_minValue;
    }

    public long getMaxValue() {
        return m_maxValue;
    }
}
