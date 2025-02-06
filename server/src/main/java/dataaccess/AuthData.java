package dataaccess;
import java.util.UUID;

public class AuthData {
    public static String generateToken(){
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

}
