fun foo() {
    myFor@
    for (i in 1..10) {
        myWhile@
        while (x()) {
            myDo@
            do {
                break@my<caret>
            } while (y())
        }
    }
}

// IGNORE_K2
// EXIST: { lookupString: "break@myDo", itemText: "break", tailText: "@myDo", attributes: "bold" }
// EXIST: { lookupString: "break@myWhile", itemText: "break", tailText: "@myWhile", attributes: "bold" }
// EXIST: { lookupString: "break@myFor", itemText: "break", tailText: "@myFor", attributes: "bold" }
// NOTHING_ELSE
