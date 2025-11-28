package ch10;

class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        super(null, null, false, false); // is this necessary?
        this.value = value;
    }
}