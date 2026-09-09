package deyadecember.stock;

public record Stock(int available, int reserved) {

    public Stock {
        if (available < 0 || reserved < 0) {
            throw new IllegalArgumentException(
                    "Stock cannot be negative: available=" + available + ", reserved=" + reserved);
        }
    }
}
