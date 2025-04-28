package auth;

public class CaptchaGenerator {
    private final int start;
    private final int increment;
    private final int expectedAnswer;

    // Constructor: User provides start and increment
    public CaptchaGenerator(int start, int increment) {
        this.start = start;
        this.increment = increment;
        this.expectedAnswer = start + (2 * increment); // Iterations fixed at 3
    }

    // Generate the captcha question (e.g., "2, 4, _")
    public String getCaptchaQuestion() {
        return String.format("%d, %d, _", start, start + increment);
    }

    // Validate the user's answer
    public boolean validateCaptcha(int userAnswer) {
        return userAnswer == expectedAnswer;
    }
}