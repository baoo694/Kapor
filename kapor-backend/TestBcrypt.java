import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$3YT0aCAH15JXr/OT3Fm9z.MBUcKOxKpv8APWtaJqPYudw.PT5.n4i";
        String raw = "Test1234!";
        System.out.println("Matches: " + encoder.matches(raw, hash));
    }
}
