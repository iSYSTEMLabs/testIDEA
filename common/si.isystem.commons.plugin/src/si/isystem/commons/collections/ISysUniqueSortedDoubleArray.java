package si.isystem.commons.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class ISysUniqueSortedDoubleArray {
    private static final int INITIAL_ARRAY_SIZE = 1 << 12;

    private double[] m_array = new double[INITIAL_ARRAY_SIZE];
    private int m_usedSlots = 0;
    
    public void add(double element) {
        // Get index for new element.
        // Last element greater or equal to the new element - so we place the new element left to it.
        // We have to shift everything right in the list from this index to right by 1.
        int index = -1;
        if (m_usedSlots == 0) { // First element
            index = 0;
        }
        else { // progressively shift left until we find a position
            index = m_usedSlots;
            do {
                // Duplicate
                if (m_array[index-1] == element) {
                    return;
                }
                // Elements are smaller from here on - we found our spot
                if (m_array[index-1] < element) {
                    break;
                }
                // Left element is still greater - go left
                if (m_array[index-1] > element) {
                    index--;
                }
                // Safety - end of list
                if (index == 0) {
                    break;
                }
            } while (true);
        }
        
        // Make space if we have to
        if (m_usedSlots == m_array.length) {
            double[] tmp = new double[m_array.length * 2];
            System.arraycopy(m_array, 0, tmp, 0, m_usedSlots);
            m_array = tmp;
        }
        
        // Shift right
        System.arraycopy(m_array, index, m_array, index+1, m_usedSlots-index);
        // Write element
        m_array[index] = element;
        m_usedSlots++;
    }
    
    public void addAll(Collection<Double> collection) {
        for (Double e : collection) {
            add(e);
        }
    }
    
    public void addAll(double[] array) {
        for (Double element : array) {
            add(element);
        }
    }
    
    public int size() {
        return m_usedSlots;
    }
    
    public double get(int idx) {
        return m_array[idx];
    }
    
    public double[] toArray() {
        double[] newA = new double[m_usedSlots];
        System.arraycopy(m_array, 0, newA, 0, m_usedSlots);
        return newA;
    }

    public void clear() {
        m_array = new double[INITIAL_ARRAY_SIZE];
        m_usedSlots = 0;
    }
    
    public static void main(String[] args) {
        test(new double[] {}, new double[] {});
        test(new double[] {5}, new double[] {5});

        test(new double[] {5,5,5,5,5}, new double[] {5});
        test(new double[] {5,5,5,3,3,3,3}, new double[] {3,5});
        
        test(new double[] {1,5,9}, new double[] {1,5,9});
        test(new double[] {1,5,9,9}, new double[] {1,5,9});
        test(new double[] {1,5,9,5}, new double[] {1,5,9});
        test(new double[] {1,5,9,1}, new double[] {1,5,9});

        test(new double[] {1,5,9,10}, new double[] {1,5,9,10});
        test(new double[] {1,5,9,8}, new double[] {1,5,8,9});
        test(new double[] {1,5,9,3}, new double[] {1,3,5,9});
        test(new double[] {1,5,9,0}, new double[] {0,1,5,9});
        test(new double[] {1,5,9,-5}, new double[] {-5,1,5,9});
    
        for (int i = 0; i < 1000; i++) {
            randomTest((int)(Math.random()*100), (int)(Math.random()*100));
        }
    }

    private static void randomTest(int maxCount, int maxSize) {
        int count = (int)(Math.random()*maxCount);
        double[] in = new double[count];
        for (int i = 0; i < count; i++) {
            in[i] = (int)(Math.random()*maxSize);
        }

        Set<Double> sortedSet = new TreeSet<>();
        for (int i = 0; i < count; i++) {
            sortedSet.add(in[i]);
        }
        
        int i = 0;
        double[] expected = new double[sortedSet.size()];
        for (Double el : sortedSet) {
            expected[i++] = el;
        }
        
        test(in, expected);
    }

    private static void test(double[] in, double expected[]) {
        ISysUniqueSortedDoubleArray s = new ISysUniqueSortedDoubleArray();
        for (double el : in) {
            s.add(el);
        }
        double[] out = s.toArray();
        
        System.out.format("%s ==>> \n%s expecting \n%s\n", Arrays.toString(in), Arrays.toString(out), Arrays.toString(expected));
        
        if (s.size() != expected.length) {
            System.out.format("ERROR: Invalid length %d\n", out.length);
            System.exit(0);
        }
        
        for (int i = 0; i < out.length; i++) {
            if (out[i] != expected[i]) {
                System.out.format("ERROR: Invalid element value at [%d] = %f (expected %f)\n", i, out[i], expected[i]);
                System.exit(0);
            }
        }
        
        for (int i = 0; i < out.length-1; i++) {
            if (!(out[i] < out[i+1])) {
                System.out.format("ERROR: List is not ordered %s\n", Arrays.toString(out));
                System.exit(0);
            }
        }
        
        System.out.format("OK\n");
    }
}
