// IGNORE_K2
import java.util.*;

class IteratorTest {

    Map<String, String> mutableMap1 = new HashMap<>();
    Map<String, String> mutableMap2 = new HashMap<>();

    void testFields() {
        mutableMap1.values().add("")
        mutableMap2.entrySet().iterator().remove();
    }

    void testFunctionParameters(Collection<String> immutableCollection, List<Integer> mutableList) {
        for (Iterator<String> it = immutableCollection.iterator(); it.hasNext(); ) {
            it.next();
        }
        mutableList.listIterator().add(2);
    }
}