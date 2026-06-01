import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TestChrono {
    public static void main(String[] args) {
        long days = ChronoUnit.DAYS.between(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
        );
        System.out.println("Days between: " + days);
    }
}