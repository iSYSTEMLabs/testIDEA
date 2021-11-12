package si.isystem.itest.wizards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import si.isystem.itest.wizards.TCGenOccur.EOccurenceType;

/**
 * Can't be run as SWTBot test, because then it does not have access to 
 * protected and package methods.
 *  
 * @author markok
 *
 */
public class TestCaseGeneratorTester {

    @Test
    public void testTCGenSimple() throws Exception {
        
        
        // model without values
        
        TCGenDataModel dataModel = new TCGenDataModel();
        TCGenSection funcSection = dataModel.getFunctionSection();
        
        funcSection.addIdentifier(-1, "first");
        TCGenVectorsTableModel vectorsTableModel = new TCGenVectorsTableModel();
        vectorsTableModel.setData(funcSection);
        
        try {
            vectorsTableModel.generateVectors();
            assertFalse("Model without values should throw an exception", true);
        } catch (IllegalArgumentException ex) {
        }
        
        
        // model with 1 identifier with 1 value
        
        TCGenIdentifier ident = funcSection.getIdentifiers().get(0);
        ident.getValues().add("1");
        
        vectorsTableModel.generateVectors();
        
        assertEquals(2, vectorsTableModel.getRowCount()); // header row/col + vector
        assertEquals(2, vectorsTableModel.getColumnCount());
        assertEquals("1", vectorsTableModel.getContentAt(1, 1));
        
        
        // model with one value to occur twice
        
        funcSection.getOccurrence().setOccurrenceType(EOccurenceType.TWO);
        
        try {
            vectorsTableModel.generateVectors();
            assertFalse("Should not be able to create two vectors with one parameter with one value"
                    + "", true);
        } catch (IllegalStateException ex) {
        }

        
        // model with two values
        
        funcSection.getOccurrence().setOccurrenceType(EOccurenceType.ONE);
        ident.getValues().add("2");

        vectorsTableModel.generateVectors();
        assertEquals(3, vectorsTableModel.getRowCount());
        assertEquals(2, vectorsTableModel.getColumnCount());
        assertEquals("1", vectorsTableModel.getContentAt(1, 1));
        assertEquals("2", vectorsTableModel.getContentAt(1, 2));
        
        
        // model with two identifiers, second one without values
        
        funcSection.addIdentifier(-1, "second");

        try {
            vectorsTableModel.generateVectors();
            assertFalse("Identifier without values should throw an exception", true);
        } catch (IllegalArgumentException ex) {
        }


        // model with two identifiers, second one with one value
        TCGenIdentifier ident2 = funcSection.getIdentifiers().get(1);
        ident2.getValues().add("21");
        
        vectorsTableModel.generateVectors();
        assertEquals(3, vectorsTableModel.getRowCount());
        assertEquals(3, vectorsTableModel.getColumnCount());
        assertEquals("1", vectorsTableModel.getContentAt(1, 1));
        assertEquals("21", vectorsTableModel.getContentAt(2, 1));
        assertEquals("2", vectorsTableModel.getContentAt(1, 2));
        assertEquals("21", vectorsTableModel.getContentAt(2, 2));
    }
    
    
    @Test
    public void testTCGenComplex() throws Exception {

        
        // model with two identifiers with occurrence set on section level
        
        TCGenDataModel dataModel = new TCGenDataModel();
        TCGenSection funcSection = dataModel.getFunctionSection();
        
        TCGenVectorsTableModel vectorsTableModel = new TCGenVectorsTableModel();
        vectorsTableModel.setData(funcSection);
        
        funcSection.getOccurrence().setValue("2");
        
        funcSection.addIdentifier(-1, "first");
        TCGenIdentifier ident1 = funcSection.getIdentifiers().get(0);
        
        funcSection.addIdentifier(-1, "second");
        TCGenIdentifier ident2 = funcSection.getIdentifiers().get(1);

        ident1.getOccurrence().setOccurrenceType(EOccurenceType.ONE); // overridden by section setting
        ident1.setRangeStart("3");
        ident1.setRangeEnd("7");
        ident1.setRangeStep("3");  // values 3, 6
        
        ident2.setOccurrence("4");   // overridden by section setting
        ident2.setRangeStart("20");
        ident2.setRangeEnd("41");
        ident2.setRangeStep("10");  // values 20 ,30, 40
        
        vectorsTableModel.generateVectors();
        assertEquals(7, vectorsTableModel.getRowCount());
        assertEquals(3, vectorsTableModel.getColumnCount());
        assertEquals("3", vectorsTableModel.getContentAt(1, 1));
        assertEquals("20", vectorsTableModel.getContentAt(2, 1));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 2));
        assertEquals("30", vectorsTableModel.getContentAt(2, 2));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 3));
        assertEquals("30", vectorsTableModel.getContentAt(2, 3));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 4));
        assertEquals("40", vectorsTableModel.getContentAt(2, 4));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 5));
        assertEquals("20", vectorsTableModel.getContentAt(2, 5));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 6));
        assertEquals("40", vectorsTableModel.getContentAt(2, 6));
        
        
        // model with two identifiers with occurrence set on identifier level

        ident2.setRangeEnd("61");
        funcSection.getOccurrence().setOccurrenceType(EOccurenceType.CUSTOM);
        ident1.getOccurrence().setOccurrenceType(EOccurenceType.THREE);
        ident2.getOccurrence().setOccurrenceType(EOccurenceType.ONE);
        
        vectorsTableModel.generateVectors();
        assertEquals(8, vectorsTableModel.getRowCount());
        assertEquals(3, vectorsTableModel.getColumnCount());
        assertEquals("3", vectorsTableModel.getContentAt(1, 1));
        assertEquals("20", vectorsTableModel.getContentAt(2, 1));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 2));
        assertEquals("30", vectorsTableModel.getContentAt(2, 2));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 3));
        assertEquals("40", vectorsTableModel.getContentAt(2, 3));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 4));
        assertEquals("30", vectorsTableModel.getContentAt(2, 4));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 5));
        assertEquals("40", vectorsTableModel.getContentAt(2, 5));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 6));
        assertEquals("50", vectorsTableModel.getContentAt(2, 6));

        assertEquals("3", vectorsTableModel.getContentAt(1, 7));
        assertEquals("60", vectorsTableModel.getContentAt(2, 7));
        
        
        // model with two identifiers with custom occurrence
        ident1.getOccurrence().setOccurrenceType(EOccurenceType.CUSTOM);
        
        ident1.addAutoValueOccurrence(0);
        ident1.addAutoValueOccurrence(1);

        ident1.getCustomValueOccurrence(0).getOccurrence().setValue("4");
        ident1.getCustomValueOccurrence(1).getOccurrence().setValue("2");

        ident2.getOccurrence().setOccurrenceType(EOccurenceType.CUSTOM);
        ident2.addAutoValueOccurrence(0);
        ident2.addAutoValueOccurrence(1);
        ident2.addAutoValueOccurrence(2);

        ident2.getCustomValueOccurrence(0).getOccurrence().setValue("1");
        ident2.getCustomValueOccurrence(1).getOccurrence().setValue("1");
        ident2.getCustomValueOccurrence(2).setValue(TCGenIdentifier.OTHER_VALUES_STR);
        ident2.getCustomValueOccurrence(2).getOccurrence().setValue("2");

        vectorsTableModel.generateVectors();
        assertEquals(10, vectorsTableModel.getRowCount());
        assertEquals(3, vectorsTableModel.getColumnCount());
        assertEquals("3", vectorsTableModel.getContentAt(1, 1));
        assertEquals("20", vectorsTableModel.getContentAt(2, 1));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 2));
        assertEquals("30", vectorsTableModel.getContentAt(2, 2));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 3));
        assertEquals("40", vectorsTableModel.getContentAt(2, 3));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 4));
        assertEquals("50", vectorsTableModel.getContentAt(2, 4));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 5));
        assertEquals("30", vectorsTableModel.getContentAt(2, 5));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 6));
        assertEquals("40", vectorsTableModel.getContentAt(2, 6));

        assertEquals("6", vectorsTableModel.getContentAt(1, 7));
        assertEquals("50", vectorsTableModel.getContentAt(2, 7));
        
        assertEquals("3", vectorsTableModel.getContentAt(1, 8));
        assertEquals("60", vectorsTableModel.getContentAt(2, 8));
        
        assertEquals("6", vectorsTableModel.getContentAt(1, 9));
        assertEquals("60", vectorsTableModel.getContentAt(2, 9));
        
        // detect too many vectors
        ident1.getOccurrence().setOccurrenceType(EOccurenceType.ONE);
        ident1.setRangeStart("-5000");
        ident1.setRangeEnd("5002");
        ident1.setRangeStep("");
        
        ident2.setOccurrence("1");
        ident2.setValues("1");
        ident2.setRangeStart("");
        ident2.setRangeEnd("");
        ident2.setRangeStep("");
        try {
            vectorsTableModel.generateVectors();
            assertFalse("Too many generated test cases should throw an exception", true);
        } catch (IllegalArgumentException ex) {
        }
    }    
}
