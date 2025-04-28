package auth;

/**
 * Representing a user in the system with security QA.
 */
public class User {
    private String userID;
    private String password;
    private String securityQuestion;
    private String securityAnswer;

    /**
    * Constructor for the User class.
    * @param userID The user's unique identifier.
    * @param password The hashed password of the user.
    * @param securityQuestion The user's security question.
    * @param securityAnswer The hashed answer to the security question.
    */
    public User (String userID, String password,String securityQuestion, String securityAnswer ) {
        this.userID = userID;
        this.password = password;
        this.securityQuestion = securityQuestion;
        this.securityAnswer = securityAnswer;
    }


    // Return the user ID.
    public String getUserId() {
        return userID;
    }

    // Return the password (in hash)
    public String getPassword() {
        return password;
    }

    // Return the security question
    public String getSecurityQuestion() {
        return securityQuestion;
    }

    // Return the security answer (in hash)
    public String getSecurityAnswer() {
        return securityAnswer;
    }
}
