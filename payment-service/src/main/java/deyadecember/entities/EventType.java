package deyadecember.entities;

public enum EventType {
    PAYMENT_COMPLETED("payments.completed"),
    PAYMENT_FAILED("payments.failed"),
    PAYMENT_REFUNDED("payments.refunded");

    private final String topic;
    EventType(String topic) { this.topic = topic; }
    public String topic() { return topic; }
}