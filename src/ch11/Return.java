package ch11;

// no changes from previous chapter

class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        // super(null, null, false, false); // in book, but I think unnecessary (not sure)
        this.value = value;
    }
}