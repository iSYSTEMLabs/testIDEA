package si.isystem.connect.data;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import si.isystem.ui.utils.ExceptionsTest;

public class JFunctionTest {

    @Before
    public void setUp() throws Exception {
        ExceptionsTest.loadLibrary();        
    }
    
    
    @Test
    public void testParseQualifiedName() {
        StringBuilder module = new StringBuilder();
        StringBuilder scopedName = new StringBuilder();
        StringBuilder signature = new StringBuilder();
        StringBuilder partition = new StringBuilder();
        
        String qName = "func1";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("", module.toString());
        assertEquals("func1", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("", partition.toString());
        
        qName = "func1,,dl.elf";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("", module.toString());
        assertEquals("func1", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("dl.elf", partition.toString());
        
        qName = "\"main.c\"#func1,,dl.elf";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("main.c", module.toString());
        assertEquals("func1", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("dl.elf", partition.toString());
        
        qName = "\"main.c\"#func1(long),,dl.elf";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("main.c", module.toString());
        assertEquals("func1", scopedName.toString());
        assertEquals("(long)", signature.toString());
        assertEquals("dl.elf", partition.toString());
        
        qName = "func1(int)";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("", module.toString());
        assertEquals("func1", scopedName.toString());
        assertEquals("(int)", signature.toString());
        assertEquals("", partition.toString());
        
        qName = "\"test1.cpp#N::N::A::B::f(int i, mytype::cc x)##\"A::s_n(int &x[], (char) *y),,test.elf";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("A::s_n", scopedName.toString());
        assertEquals("(int &x[], (char) *y)", signature.toString());
        assertEquals("test.elf", partition.toString());
        
        qName = "\"test1.cpp#N::N::A::B::f(int i, mytype::cc x)##\"s_n,,test.elf";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("s_n", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("test.elf", partition.toString());

        // no quotes
        qName = "test1.cpp#s_n";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("s_n", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("", partition.toString());
        
        // quotes in the old way
        qName = "\"test1.cpp\"#s_n";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("s_n", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("", partition.toString());
        
        // quotes according to new spec
        qName = "\"test1.cpp#\"s_n";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("s_n", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("", partition.toString());
        
        qName = "\"test1.cpp#f()##\"s_n";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("s_n", scopedName.toString());
        assertEquals("", signature.toString());
        assertEquals("", partition.toString());
        
        
        qName = "__cxxabiv1::__forced_unwind::~__forced_unwind(),,stm32.elf";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("", module.toString());
        assertEquals("__cxxabiv1::__forced_unwind::~__forced_unwind", scopedName.toString());
        assertEquals("()", signature.toString());
        assertEquals("stm32.elf", partition.toString());
        
        // quoted overloaded function according to old spec
        qName = "\"test1.cpp\"#\"f(int i, mytype::cc x)\"";
        JFunction.parseQualifiedName(qName, module, scopedName, signature, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("f", scopedName.toString());
        assertEquals("(int i, mytype::cc x)", signature.toString());
        assertEquals("", partition.toString());
        
        // variable
        qName = "\"test1.cpp#\"array[3],,x.elf";
        JVariable.parseQualifiedName(qName, module, scopedName, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("array[3]", scopedName.toString());
        assertEquals("x.elf", partition.toString());
        
        qName = "\"test1.cpp#\"array[3]";
        JVariable.parseQualifiedName(qName, module, scopedName, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("array[3]", scopedName.toString());
        assertEquals("", partition.toString());
        
        qName = "\"test1.cpp#\"p->x";
        JVariable.parseQualifiedName(qName, module, scopedName, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("p->x", scopedName.toString());
        assertEquals("", partition.toString());
        
        qName = "\"test1.cpp#\"p.x";
        JVariable.parseQualifiedName(qName, module, scopedName, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("p.x", scopedName.toString());
        assertEquals("", partition.toString());
        
        qName = "\"test1.cpp#\"*p";
        JVariable.parseQualifiedName(qName, module, scopedName, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("*p", scopedName.toString());
        assertEquals("", partition.toString());

        qName = "\"test1.cpp#\"*(char *)p";
        JVariable.parseQualifiedName(qName, module, scopedName, partition);
        assertEquals("test1.cpp", module.toString());
        assertEquals("*(char *)p", scopedName.toString());
        assertEquals("", partition.toString());

        qName = "__cxxabiv1::__forced_unwind::~__forced_unwind(),,stm32.elf"; // from stm23.elf
        JVariable.parseQualifiedName(qName, module, scopedName, partition);
        assertEquals("", module.toString());
        assertEquals("__cxxabiv1::__forced_unwind::~__forced_unwind", scopedName.toString());
        assertEquals("stm32.elf", partition.toString());
}
}
