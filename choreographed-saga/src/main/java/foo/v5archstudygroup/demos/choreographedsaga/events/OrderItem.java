package foo.v5archstudygroup.demos.choreographedsaga.events;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String description,
        int quantity,
        BigDecimal itemPrice,
        BigDecimal vatPercentage
) {
}
